package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.AdminActionLogResponse;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminActionLogService {

    static final int DEFAULT_LIMIT = 100;
    static final int MAX_LIMIT = 500;

    private final AdminActionLogRepository logRepository;
    private final TeamRepository teamRepository;
    private final QuizRepository quizRepository;

    public AdminActionLogService(AdminActionLogRepository logRepository, TeamRepository teamRepository,
                                  QuizRepository quizRepository) {
        this.logRepository = logRepository;
        this.teamRepository = teamRepository;
        this.quizRepository = quizRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminActionLogResponse> list(Long teamId, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        PageRequest page = PageRequest.of(0, size);
        List<AdminActionLog> logs = teamId == null
                ? logRepository.findAllByOrderByIdDesc(page)
                : logRepository.findByTeamIdOrderByIdDesc(teamId, page);

        Map<Long, String> teamNames = teamRepository.findAllById(
                        logs.stream().map(AdminActionLog::getTeamId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Team::getId, Team::getName));
        Map<Long, String> quizTitles = quizRepository.findAllById(
                        logs.stream().map(AdminActionLog::getQuizId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Quiz::getId, Quiz::getTitle));

        return logs.stream()
                .map(l -> new AdminActionLogResponse(l.getId(), l.getAdminUsername(), l.getAction(),
                        l.getTeamId(), teamNames.get(l.getTeamId()),
                        l.getQuizId(), quizTitles.get(l.getQuizId()),
                        l.getDetail(), l.getCreatedAt()))
                .toList();
    }
}
