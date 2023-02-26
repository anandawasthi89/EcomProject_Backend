package com.project.ecomapp.ecommerce_Project.Controller;

import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = "/Users")
public class UserController {

    @Autowired
    private CustomUserDetailsService userService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @GetMapping(path = "/AllUsers")
    public List<User> getAllUsers(){
        return this.userService.getAllUsers();
    }

    @GetMapping(path = "/currentuser")
    public CustomUserDetails getUserDetails(Principal principal){
        System.out.println("hello "+principal.getName());
        return (CustomUserDetails)userService.loadUserByUsername(principal.getName());
    }

    @PostMapping(path = "/addUser")
    public User addNewUser(@RequestBody User user){
        System.out.println(user.getUID()+" "+user.getName()+" "+user.getEmail()+" "+user.getPassword());
        user.setPassword(this.bCryptPasswordEncoder.encode(user.getPassword()));
        return this.userService.addNewUser(user);

    }

    @PutMapping(path = "/UpdateExistingUser")
    public User updateExistingUser(@RequestBody User user){
        return this.userService.updateExistingUser(user);
    }

    @DeleteMapping(path = "/deleteUser/{id}")
    public void deleteExistingUser(@PathVariable int id){
        this.userService.deleteExistingUser(id);
    }

}
