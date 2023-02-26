package com.scm.controller;

import java.security.Principal;
import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.scm.dao.UserRepository;
import com.scm.entities.User;
import com.scm.helper.Message;
import com.scm.service.EmailService;

@Controller
public class ForgotController {

	@Autowired
	private EmailService emailServiceImpl;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	Random random = new Random(1000);

	// email id form open handler
	@GetMapping("/forgot")
	public String openEmailForm() {

		return "forgot-email-form";
	}

	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, HttpSession httpSession) {

		System.out.println("Email : " + email);

		// Generating otp of 6 digit
		int otp = random.nextInt(999999);
		System.out.println("OTP : " + otp);

		// Write code to send otp on email
		String to = email;
		String message = "Please refer the OTP for forgot password confirmation : " + otp;
		String subject = "OTP for Smart Contact Manager";

		boolean flag = emailServiceImpl.sendMail(to, message, subject);

		if (flag) {
			httpSession.setAttribute("oldOtp", otp);
			httpSession.setAttribute("email", email);
			return "verify-otp";
		} else {
			httpSession.setAttribute("message", "Check your email ID");
			return "forgot-email-form";
		}
	}

	// process otp
	@PostMapping("/otp-verification")
	public String otpVerification(@RequestParam("otp") int otp, HttpSession session) {
		System.out.println("OTP : " + otp);
		int oldOtp = (int) session.getAttribute("oldOtp");
		String email = (String) session.getAttribute("email");

		if (oldOtp == otp) {
			// password change from
			User user = userRepository.getUserByName(email);

			if (user == null) {
				// send error message
				session.setAttribute("message", "User does not exist with this email");
				return "forgot-email-form";

			} else {
				// send change password form
				return "password-change-form";
			}
		} else {
			session.setAttribute("message", "You have entered wrong OTP");
			return "verify-otp";
		}
	}

	// Change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Model model, Principal principal, HttpSession session) {

		// user
		String name = principal.getName();
		User currentUser = userRepository.getUserByName(name);

		if (bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// Change Password
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			session.setAttribute("message", new Message("Password Successfully Changed", "success"));
		} else {
			// Error
			session.setAttribute("message", new Message("Entered Wrong old password", "danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

	// Password change handler
	@PostMapping("/changePassword")
	public String passwordChange(@RequestParam("newPassword") String newPassword, HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = userRepository.getUserByName(email);
		user.setPassword(bCryptPasswordEncoder.encode(newPassword));
		userRepository.save(user);
		return "redirect:/signin?change=Password changed successfully..";

	}

}
