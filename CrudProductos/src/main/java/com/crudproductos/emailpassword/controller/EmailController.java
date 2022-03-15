package com.crudproductos.emailpassword.controller;

import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crudproductos.dto.Mensaje;
import com.crudproductos.emailpassword.dto.ChangePasswordDto;
import com.crudproductos.emailpassword.dto.EmailDto;
import com.crudproductos.emailpassword.service.EmailService;
import com.crudproductos.security.entity.Usuario;
import com.crudproductos.security.service.UsuarioService;

@RestController
@RequestMapping("/email-password")
@CrossOrigin
public class EmailController {

	@Autowired
	EmailService emailService;
	
	@Value("${spring.mail.username}")
	private String mailFrom;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
    PasswordEncoder passwordEncoder;
	
	@PostMapping("/send-email")
	public ResponseEntity<?> sendEmailTemplate(@RequestBody EmailDto dto){
		Optional<Usuario> optional = usuarioService.getByNombreUsuarioOrEmail(dto.getMailTo());
		if(!optional.isPresent())
			return new ResponseEntity(new Mensaje("No existe ningún usuario con esas credenciales"),HttpStatus.NOT_FOUND);
		Usuario usuario = optional.get();
		dto.setMailFrom(mailFrom);
		dto.setMailTo(usuario.getEmail());
		dto.setUserName(usuario.getNombreUsuario());
		dto.setSubject("Cambio de contraseña");
		UUID uuid = UUID.randomUUID();
		String tokenPassword = uuid.toString();
		dto.setTokenPassword(tokenPassword);
		usuario.setTokenPassword(tokenPassword);
		usuarioService.save(usuario);
		emailService.sendEmail(dto);
		return new ResponseEntity(new Mensaje("Te hemos enviado un correo"),HttpStatus.OK);
	}
	
	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto passwordDto, BindingResult result){
		if(result.hasErrors())
			return new ResponseEntity(new Mensaje("Campos mal puestos"),HttpStatus.BAD_REQUEST);
		if(!passwordDto.getPassword().equals(passwordDto.getConfirmPassword()))
			return new ResponseEntity(new Mensaje("Las contraseñas no coinciden"),HttpStatus.BAD_REQUEST);
		Optional<Usuario> optional = usuarioService.getByTokenPassword(passwordDto.getTokenPassword());
		if(!optional.isPresent())
			return new ResponseEntity(new Mensaje("No existe ningún usuario con esas credenciales"),HttpStatus.NOT_FOUND);
		Usuario usuario = optional.get();
		String newPassword = passwordEncoder.encode(passwordDto.getPassword());
		usuario.setPassword(newPassword);
		usuario.setTokenPassword(null);
		usuarioService.save(usuario);
		return new ResponseEntity(new Mensaje("Contraseña actualizada"),HttpStatus.OK);		
	}
}
