package com.project.ecomapp.ecommerce_Project.Services;

import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Repository.UserRepDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepDAO userRepDAO;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepDAO.findByEmail(email);
        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }
        return new CustomUserDetails(user);
    }

    public List<User> getAllUsers(){
        return userRepDAO.findAll();
    }

    public Optional<User> findUser(int id){
        Optional<User> u=this.userRepDAO.findById(id);
        return u;
    }

    public User addNewUser(User user){
        return  userRepDAO.save(user);
    }

    public User updateExistingUser(User user){
        return userRepDAO.save(user);
    }

    public void deleteExistingUser(int id){
        User u=null;
        if(this.findUser(id).isPresent()){
            u=this.findUser(id).get();}
        if(u != null)
            userRepDAO.delete(u);
    }

}
