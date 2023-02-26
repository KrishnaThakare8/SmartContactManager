package com.scm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender javaMailSender;

	// To send a simple email
	public boolean sendMail(String to, String message, String subject) {

		// Try block to check for exceptions
		try {
			
			// Creating a simple mail message
			SimpleMailMessage mailMessage = new SimpleMailMessage();

			// Setting up necessary details
			mailMessage.setTo(to);
			mailMessage.setText(message);
			mailMessage.setSubject(subject);

			// Sending the mail
			javaMailSender.send(mailMessage);
			return true;
		}

		// Catch block to handle the exceptions
		catch (Exception e) {
			return false;
		}
	}

}
