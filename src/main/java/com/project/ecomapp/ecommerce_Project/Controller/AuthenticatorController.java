package com.project.ecomapp.ecommerce_Project.Controller;

import com.mysql.cj.protocol.AuthenticationProvider;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTResponse;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class AuthenticatorController {

    @Autowired
    private DaoAuthenticationProvider authenticationProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JWTUtils jwtUtils;

    //generate token
    @PostMapping("/generateToken")
    public ResponseEntity<?> generateToken(@RequestBody UserJWTRequest jwtRequest) throws Exception {
        try{
            authenticate(jwtRequest.getEmail(),jwtRequest.getPassword());
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("User not found");
        }

        UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(jwtRequest.getEmail());
        String token = this.jwtUtils.generateToken(userDetails);
        return ResponseEntity.ok(new UserJWTResponse(token));


    }

    private void authenticate(String username,String password) throws Exception {

        try {

            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(username,password));

        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("error " + e.getMessage());
        }


    }


}
