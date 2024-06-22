package com.ecommerce.Service;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.model.User;
import com.ecommerce.model.VerificationToken;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.VerificationTokenRepository;

@Service
public class UserService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VerificationTokenRepository verificationTokenRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<User> user = userRepository.findByEmail(username);
		if (user.isPresent()) {
			var userObj = user.get();
			return org.springframework.security.core.userdetails.User.builder().username(userObj.getEmail())
					.password(userObj.getPassword()).roles(getRoles(userObj)).build();
		} else {
			throw new UsernameNotFoundException(username);
		}
	}

	private String[] getRoles(User user) {
		if (user.getRole() == null) {
		    return new String[]{"USER"};
		}
		return user.getRole().split(",");
	}
	
	public void confirmUser(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
        if (verificationToken != null && verificationToken.getExpiryDate().after(new Date())) {
            User user = userRepository.findByEmail(verificationToken.getEmail()).get();
            user.setEnabled(true);
            userRepository.save(user);
            verificationTokenRepository.delete(verificationToken);
        }
    }
}
