package com.fstg.mediatech.controllers;

import com.fstg.mediatech.entities.PasswordResetToken;
import com.fstg.mediatech.entities.User;
import com.fstg.mediatech.repositories.PasswordResetTokenRepository;
import com.fstg.mediatech.repositories.UserRepository;
import com.fstg.mediatech.services.EmailService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
@AllArgsConstructor
public class AuthController {

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	
	private final EmailService emailService;
	
	private static final String uploadPath = "uploads/images/";

	@ModelAttribute
	public void addUserToModel(Model model, Principal principal) {
		if (principal != null) {
			Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
			if(optionalUser.isPresent()){
				model.addAttribute("user", optionalUser.get());
			}
		}
	}

	@GetMapping("/signup")
	public String showSignUpForm(Model model) {
		model.addAttribute("user", new User());
		return "signup";
	}

	@PostMapping("/register")
	public String registerUser(@ModelAttribute User user) {
		user.setRoles(Set.of("ROLE_USER"));
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userRepository.save(user);
		return "redirect:/login";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/forgot-password")
	public String showForgotPasswordForm() {
		//model.addAttribute("email", "");
		return "forgot-password";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
	    Optional<User> userOptional = userRepository.findByUsername(email);

	    if (userOptional.isPresent()) {
	        User user = userOptional.get();

	        // Generate token
	        String token = UUID.randomUUID().toString();

	        // Save reset token
	        PasswordResetToken resetToken = new PasswordResetToken();
	        resetToken.setToken(token);
	        resetToken.setUser(user);
	        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
	        passwordResetTokenRepository.save(resetToken);

	        // Send email
	        emailService.sendPasswordResetEmail(user.getUsername(), token);

	        // Success message using RedirectAttributes
	        redirectAttributes.addFlashAttribute("message", "A reset link has been sent to your email.");
	        return "redirect:/forgot-password"; // Redirect to show the message
	    } else {
	        redirectAttributes.addFlashAttribute("error", "Account not found!");
	        return "redirect:/forgot-password"; // Redirect to show the error
	    }
	}

	@PostMapping("/reset-password")
	public String handlePasswordReset(@RequestParam String token, @RequestParam String password, @RequestParam String confirmPassword, Model model) {
		if (!password.equals(confirmPassword)) {
			model.addAttribute("error", "Passwords do not match.");
			model.addAttribute("token", token); // So the form keeps the token
			return "reset-password";
		}
		Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);

		if (tokenOptional.isPresent() && tokenOptional.get().getExpiryDate().isAfter(LocalDateTime.now())) {
			User user = tokenOptional.get().getUser();
			user.setPassword(passwordEncoder.encode(password));
			userRepository.save(user);
			passwordResetTokenRepository.delete(tokenOptional.get());

			return "redirect:/login";
		}
		model.addAttribute("error", "token expired!");
		model.addAttribute("token", token); // So the form keeps the token
		return "reset-password";
	}

	@GetMapping("/reset-password")
	public String showResetPasswordForm(@RequestParam String token, Model model) {
		model.addAttribute("token", token);
		return "reset-password";
	}


	@GetMapping("/profile/edit")
	public String showEditForm(Model model, Principal principal) {
		//User user = userRepository.findByUsername(principal.getName()).get();
		//model.addAttribute("user", user);
		return "edit-profile";
	}

	@PostMapping("/profile/edit")
	public String updateProfile(@ModelAttribute User userForm,
								Principal principal,
								Model model) throws IOException {
		User user = userRepository.findByUsername(principal.getName()).orElseThrow();

		if(Strings.isNotBlank(userForm.getName()))
			user.setName(userForm.getName());

		if(Strings.isNotBlank(userForm.getUsername()))
			user.setUsername(userForm.getUsername());

		if (!userForm.getImage().isEmpty()) {

			if (Strings.isNotBlank(user.getImagePath())) {
				Path oldImagePath = Paths.get(uploadPath, user.getImagePath());
				if (Files.exists(oldImagePath)) {
					Files.delete(oldImagePath);  // Delete the old image file
				}
			}

			MultipartFile imageFile = userForm.getImage();
			String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(imageFile.getInputStream(), filePath);
			user.setImagePath(fileName);
		}

		userRepository.save(user);
		model.addAttribute("message", "Profile updated!");
		model.addAttribute("user", user);
		return "edit-profile";
	}
	
}
