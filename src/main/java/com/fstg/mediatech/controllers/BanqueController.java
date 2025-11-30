package com.fstg.mediatech.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.fstg.mediatech.entities.User;
import com.fstg.mediatech.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fstg.mediatech.repositories.CompteRepository;
import com.fstg.mediatech.entities.Compte;

@Controller
@AllArgsConstructor
public class BanqueController {
	
	private final CompteRepository compteRepository;
	private final UserRepository userRepository;
	
	
	 
	//@Autowired
	@ModelAttribute
	public void addUserToModel(Model model, Principal principal) {
		Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
			if(optionalUser.isPresent()){
				model.addAttribute("user", optionalUser.get());
			}
	}

	@RequestMapping("/comptes")
	public String afficher(Model model) {

		List<Compte>comptes=compteRepository.findByUser((User) model.getAttribute("user"));
		model.addAttribute("comptes", comptes);
		return "listeComptes";
	}
	

	@GetMapping("/ajouter")
	public String add(Model model) {
		model.addAttribute("compte",new Compte());
		return "ajouterCompte";
	}

	@PostMapping("/ajouter")
	public String ajouter(Model model, @ModelAttribute Compte compte) {
		compte.setUser((User) model.getAttribute("user"));
		compteRepository.save(compte);

		return "redirect:comptes";
	}

	@GetMapping("/details/{id}")
	public String supprimer(@PathVariable("id") Long id, Model model) {
		Compte compte= compteRepository.findById(id).get();

		model.addAttribute("compte",compte);

		return "detailsCompte";
	}

	@PostMapping("/comptes/{id}/retrait")
	public String retrait(@PathVariable("id") Long id, @RequestParam("montant_retrait") double montantRetrait) {

		Optional<Compte> optionalCompte = compteRepository.findById(id); // Check if the account exists

	    if (optionalCompte.isPresent()) {
	        Compte compte = optionalCompte.get();

	        if (compte.getSolde() >= montantRetrait && montantRetrait > 0) {
	            compte.setSolde(compte.getSolde() - montantRetrait);
	            compteRepository.save(compte);
	        }
	    }

	    return "redirect:../../comptes"; // Corrected redirection path
	}


	@PostMapping("/comptes/{id}/depot")
	public String depot(@PathVariable("id") Long id, @RequestParam("montant_depot") double montantDepot) {

		Optional<Compte> optionalCompte = compteRepository.findById(id); // Check if the account exists

	    if (optionalCompte.isPresent() &&  montantDepot > 0) {
	        Compte compte = optionalCompte.get();

			compte.setSolde(compte.getSolde() + montantDepot);
	        compteRepository.save(compte);
	    }

	    return "redirect:../../comptes"; 
	}

	@RequestMapping("/")
	public String home() {
		return "index";
	}

	@GetMapping("/supprimer/{id}")
	public String supprimer(@PathVariable("id") Long id) {
		compteRepository.deleteById(id);
		return "redirect:../comptes";
		
	}
}