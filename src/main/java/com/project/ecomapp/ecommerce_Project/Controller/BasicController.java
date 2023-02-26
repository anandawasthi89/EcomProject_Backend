package com.project.ecomapp.ecommerce_Project.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class BasicController {

    @GetMapping(path = "/")
    public String getHome(){ return  "Homepge";}

}
