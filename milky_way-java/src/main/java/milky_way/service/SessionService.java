package milky_way.service;

import jakarta.transaction.Transactional;
import milky_way.entity.Session;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.repository.SessionRepository;
import milky_way.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    public Optional<Session> getSession(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.flatMap(value -> sessionRepository.findByUserId(value.getId())
                .filter(session -> !session.isExpired()));
    }

    public Session createSession(User user) {
        Session session = Session.builder()
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .users(user)
                .build();
        return sessionRepository.save(session);
    }

    public Optional<Session> getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    public boolean isSessionValid(String email) {
        Optional<Session> session = sessionRepository.findByUsersEmail(email);
        return session.isPresent() && session.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    public void invalidateSession(String email) {
        Optional<Session> session = sessionRepository.findByUsersEmail(email);
        session.ifPresent(value -> sessionRepository.delete(value));
    }

    @Transactional
    public void invalidateAllSessions(String email) {
        List<Session> sessions = sessionRepository.findAllByUsersEmail(email);
        if (!sessions.isEmpty()) {
            sessionRepository.deleteAll(sessions);
        }
    }
}
