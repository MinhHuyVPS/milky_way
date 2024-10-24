package milky_way.service;

import milky_way.entity.Session;
import milky_way.entity.User;
import milky_way.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    public Session createSession(User user) {
        Session session = Session.builder()
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .user(user)
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
        Optional<Session> session = sessionRepository.findByUserEmail(email);
        return session.isPresent() && session.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    public void invalidateSession(String email) {
        Optional<Session> session = sessionRepository.findByUserEmail(email);
        session.ifPresent(value -> sessionRepository.delete(value));
    }
}
