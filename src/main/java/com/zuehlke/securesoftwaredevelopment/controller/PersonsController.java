package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.PersonRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller

public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    public String person(@PathVariable int id, Model model, HttpSession session, Authentication principal) throws AccessDeniedException {

        User user = (User) principal.getPrincipal();
        LOG.info("Person requesting update: {}", user.getUsername());

        // Check if the principal has UPDATE_PERSON authority or is updating their own profile
        boolean hasUpdateAuthority = principal.getAuthorities().contains(new SimpleGrantedAuthority("VIEW_PERSON"));
        boolean isUpdatingOwnProfile = user.getId() == id;

        if (!hasUpdateAuthority && !isUpdatingOwnProfile) {
            throw new AccessDeniedException("You do not have permission to update this person");
        }

        String csrfToken = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", csrfToken);
        model.addAttribute("person", personRepository.get("" + id));
        return "person";
    }

    @GetMapping("/myprofile")
    public String self(Model model, Authentication authentication, HttpSession session) {
        String csrfToken = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", csrfToken);
        User user = (User) authentication.getPrincipal();
        model.addAttribute("person", personRepository.get("" + user.getId()));
        return "person";
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> person(@PathVariable int id, Authentication principal) throws AccessDeniedException {

        User user = (User) principal.getPrincipal();

        boolean hasUpdateAuthority = principal.getAuthorities().contains(new SimpleGrantedAuthority("UPDATE_PERSON"));
        boolean isUpdatingOwnProfile = user.getId() == id;

        if (!hasUpdateAuthority && !isUpdatingOwnProfile) {
            throw new AccessDeniedException("You do not have permission to update this person");
        }
        personRepository.delete(id);
        userRepository.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-person")
    public String updatePerson(
            Person person,
            @RequestParam("CSRF_TOKEN") String csrfToken,
            HttpSession session,
            Authentication principal) throws AccessDeniedException {

        User user = (User) principal.getPrincipal();
        LOG.info("Person requesting update: {}", user.getUsername());

        String sessionToken = session.getAttribute("CSRF_TOKEN").toString();
        if (!sessionToken.equals(csrfToken)) {
            throw new AccessDeniedException("CSRF Token invalid");
        }

        // Check if the principal has UPDATE_PERSON authority or is updating their own profile
        boolean hasUpdateAuthority = principal.getAuthorities().contains(new SimpleGrantedAuthority("UPDATE_PERSON"));
        boolean isUpdatingOwnProfile = user.getId() == Integer.parseInt(person.getId());

        if (!hasUpdateAuthority && !isUpdatingOwnProfile) {
            throw new AccessDeniedException("You do not have permission to update this person");
        }

        personRepository.update(person);
        return "redirect:/persons/" + person.getId();
    }

    @PreAuthorize("hasAuthority('VIEW_PERSON_LIST')")
    @GetMapping("/persons")
    public String persons(Model model) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}
