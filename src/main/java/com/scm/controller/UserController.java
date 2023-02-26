package com.scm.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.scm.dao.ContactRepository;
import com.scm.dao.UserRepository;
import com.scm.entities.Contact;
import com.scm.entities.User;
import com.scm.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// Method for Adding common user data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();
		User user = userRepository.getUserByName(username);
		model.addAttribute("user", user);
	}

	// Dash-board Home
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard Design");
		return "normal/userDashboard";
	}

	// Open add form handler
	@GetMapping("/addcontact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/addcontact";
	}

	// Processing Add Contact form
	@PostMapping("/processcontact")
	public String processContact(@ModelAttribute Contact contact, Principal principal,
			@RequestParam("imageProfile") MultipartFile file, HttpSession session) throws Exception {

//		System.out.println(contact);

		try {
			// name of user
			String name = principal.getName();

			// get user by name
			User user = userRepository.getUserByName(name);

			// set user from contact due to bidirectional mapping
			contact.setUser(user);

			// Processing and Uploading file
			if (file.isEmpty()) {
				// Message to view
				System.out.println("File is Emplty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("File upload done");
			}

			// Add contact to the same user
			// getting user from contact to add contact due to bidirectional mapping
			user.getContacts().add(contact);

			// save user contact in database
			userRepository.save(user);

			System.out.println("User contact saved");

			// Success message
			session.setAttribute("message", new Message("Contact is added", "success"));

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

			// Error message
			session.setAttribute("message", new Message("Something went wrong", "danger"));
		}
		return "normal/addcontact";
	}

	// View Contact
	// per page 5 contact (Per page = 5 [n])
	// current page = 0[page]
	@GetMapping("/showcontacts/{page}")
	public String showContacts(@PathVariable("page") int page, Model model, Principal principal) {

		model.addAttribute("title", "View Contact");
		String name = principal.getName();
		User user = userRepository.getUserByName(name);

		// List of contacts
		// currentPage-page
		// contact per page
		Pageable pageable = PageRequest.of(page, 8);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentpage", page);
		model.addAttribute("totalpages", contacts.getTotalPages());

		return "normal/showcontacts";
	}

	// Showing specific contact details
	@GetMapping("/contact/{id}")
	public String showContact(@PathVariable("id") Integer id, Model model, Principal principal) {
		System.out.println(id);

		String username = principal.getName();
		User user = userRepository.getUserByName(username);

		Optional<Contact> optional = contactRepository.findById(id);
		Contact contact = optional.get();

		if (contact.getUser().getId() == user.getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", "View Contact ID: " + id);
		}
		return "normal/showcontact";
	}

	// delete contact by id
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid, Model model, HttpSession session,
			Principal principal) {

		// Getting contact by id
		Contact contact = contactRepository.findById(cid).get();

		// User Set null because contact link with user.
		// After setting user null we can delete the complete object of database
		// Delete contact .
		User user = userRepository.getUserByName(principal.getName());
		user.getContacts().remove(contact);
		userRepository.save(user);
		// Sending message to the view.
		session.setAttribute("message", new Message("Contact Deleted Successfully", "success"));

		return "redirect:/user/showcontacts/0";
	}

	// Open update form handler
	@PostMapping("/updateform/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model) {
		model.addAttribute("title", "Update Contact");

		Contact contact = contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);

		return "normal/updateform";
	}

	// update contact handler
	@PostMapping("/processupdate")
	public String updateHandler(@ModelAttribute Contact contact, Principal principal,
			@RequestParam("imageProfile") MultipartFile file, Model model, HttpSession session) {

		try {
			// old contact detail

			Contact oldContact = contactRepository.findById(contact.getCid()).get();

			if (!file.isEmpty()) {

				// delete old photo
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldContact.getImage());
				file1.delete();

				// update new photo
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {

				contact.setImage(oldContact.getImage());
			}
			User user = userRepository.getUserByName(principal.getName());
			contact.setUser(user);
			contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(contact.getName());
		System.out.println(contact.getCid());

		return "redirect:/user/contact/" + contact.getCid();
	}

	// Profile handler
	@GetMapping("/profile")
	public String profileHandler(Model model) {
		model.addAttribute("title", "Profile");
		return "normal/profile";
	}

	// Open Setting handler
	@GetMapping("/settings")
	public String openSetting(Model model) {

		model.addAttribute("title", "Settings");
		return "normal/settings";
	}

}
