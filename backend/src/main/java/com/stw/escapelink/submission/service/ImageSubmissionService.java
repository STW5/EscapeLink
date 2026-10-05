package com.stw.escapelink.submission.service;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.config.UploadProperties;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.submission.domain.ImageSubmission;
import com.stw.escapelink.submission.dto.ImageSubmissionResponse;
import com.stw.escapelink.submission.repository.ImageSubmissionRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageSubmissionService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/png", "png"
    );

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final QuizProgressRepository quizProgressRepository;
    private final ImageSubmissionRepository imageSubmissionRepository;
    private final UploadProperties uploadProperties;
    private final ApplicationEventPublisher eventPublisher;

    public ImageSubmissionService(TeamRepository teamRepository, GameRepository gameRepository,
                                   QuizRepository quizRepository, QuizProgressRepository quizProgressRepository,
                                   ImageSubmissionRepository imageSubmissionRepository,
                                   UploadProperties uploadProperties, ApplicationEventPublisher eventPublisher) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.quizRepository = quizRepository;
        this.quizProgressRepository = quizProgressRepository;
        this.imageSubmissionRepository = imageSubmissionRepository;
        this.uploadProperties = uploadProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ImageSubmissionResponse submit(Long teamId, Long quizId, MultipartFile file) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        Game game = gameRepository.findById(team.getGameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!game.isRunning()) {
            throw new BusinessException(ErrorCode.GAME_NOT_RUNNING);
        }

        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> q.getGameId().equals(game.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        if (!quiz.isImage()) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_TYPE);
        }

        int runNo = team.getCurrentRunNo();
        quizProgressRepository.insertIfAbsent(teamId, quizId, runNo);
        QuizProgress progress = quizProgressRepository
                .findByTeamIdAndQuizIdAndRunNo(teamId, quizId, runNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        if (progress.isCompleted()) {
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_COMPLETED);
        }

        String extension = validateAndDetectExtension(file);
        String filePath = storeFile(file, game.getId(), teamId, quizId, runNo, extension);

        int nextVersion = imageSubmissionRepository
                .findTopByTeamIdAndQuizIdAndRunNoOrderBySubmissionVersionDesc(teamId, quizId, runNo)
                .map(s -> s.getSubmissionVersion() + 1)
                .orElse(1);

        ImageSubmission submission = imageSubmissionRepository.save(
                new ImageSubmission(teamId, quizId, runNo, nextVersion, filePath));

        progress.markPendingReview();
        quizProgressRepository.save(progress);

        eventPublisher.publishEvent(new ImageSubmittedEvent(teamId, quizId, submission.getId()));

        return new ImageSubmissionResponse(submission.getId(), submission.getSubmissionVersion(), progress.getStatus());
    }

    private String validateAndDetectExtension(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }

        // The declared Content-Type is client-supplied and not trustworthy on its own;
        // confirm the bytes actually decode as an image before accepting the upload.
        BufferedImage decoded;
        try (InputStream in = file.getInputStream()) {
            decoded = ImageIO.read(in);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }
        if (decoded == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }

        return EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, "bin");
    }

    private String storeFile(MultipartFile file, Long gameId, Long teamId, Long quizId, int runNo, String extension) {
        Path directory = Path.of(uploadProperties.getRootPath(),
                "game-" + gameId, "team-" + teamId, "quiz-" + quizId, "run-" + runNo);
        Path destination = directory.resolve(UUID.randomUUID() + "." + extension);

        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }

        return destination.toString();
    }
}
