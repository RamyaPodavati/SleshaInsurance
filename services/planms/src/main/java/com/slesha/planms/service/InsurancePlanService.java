package com.slesha.planms.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.kafka.core.KafkaTemplate;
import javax.annotation.PostConstruct;

import com.google.gson.Gson;
import com.slesha.planms.dto.EnrollRequest;
import com.slesha.planms.entity.InsurancePlan;
import com.slesha.planms.entity.User;
import com.slesha.planms.entity.UserPlan;
import com.slesha.planms.repo.InsurancePlanRepo;
import com.slesha.planms.repo.UserPlanRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.*;

@Service
public class InsurancePlanService {

    @Autowired
    InsurancePlanRepo repo;

    @Autowired
    UserPlanRepo upRepo;

    @Autowired
    RestTemplate template;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;


    public void addPlan(InsurancePlan plan){
        repo.save(plan);
    }

    @Cacheable(value = "plan",key = "'all'")
    public List<InsurancePlan> getPlans(){
        final boolean isMarch = LocalDate.now().getMonthValue() == 3;
        if(isMarch){
            return repo.findAll().stream().map(x->{
                x.setAveragePremium((int)(x.getAveragePremium()*0.9));
                x.setMaximumCoverage((int)(x.getMaximumCoverage()*1.1));
                return x;
            }).collect(Collectors.toList());
        }
        else{
            return repo.findAll();
        }
      
    }
    public Optional<InsurancePlan> getPlan(Integer id){
        return repo.findById(id);
    }

    public Optional<User> getUser(String emailId){
        ResponseEntity<User> response=template.getForEntity("http://userms:8080/userms/"+emailId, User.class);
        if(response.getStatusCode()==HttpStatus.ACCEPTED){
            return Optional.of(response.getBody());
        }
        return Optional.ofNullable(null);
    }
    
    public List<UserPlan> enroll(EnrollRequest req){
            Optional<User> user=getUser(req.getEmailId());
            Optional<InsurancePlan> plan=getPlan(req.getPlanId());
            if(!user.isPresent() || !plan.isPresent()){
                throw new RuntimeException();
            }

            UserPlan up = new UserPlan();
            up.setUser(user.get());
            up.setAge(req.getAge());
            up.setGender(req.getGender());
            up.setSmoke(req.getSmoke());
            up.setPlan(plan.get());
            up.setType(req.getType());
            upRepo.save(up);
            upRepo.flush();
            Gson g = new Gson();
            kafkaTemplate.send("enroll",g.toJson(req));
            return upRepo.findByEmailId(up.getUser().getEmailId());
    }


    @PostConstruct
    public void setup(){
            List<InsurancePlan> plans = repo.findAll();
            if(plans.size()==0){
                InsurancePlan disability= new InsurancePlan(null, "Disability Insurance", "Health", 250, 100000);
                InsurancePlan health= new InsurancePlan(null, "Health Insurance", "Health", 150, 200000);
                InsurancePlan life= new InsurancePlan(null, "Life Insurance", "Health", 350, 400000);
                InsurancePlan auto= new InsurancePlan(null, "Auto Insurance", "Auto", 150, 100000);
                InsurancePlan home= new InsurancePlan(null, "Home Insurance", "Home", 350, 1000000);
                InsurancePlan rent= new InsurancePlan(null, "Renters Insurance", "Home", 50, 10000);
                repo.saveAll(Arrays.asList(disability,health,life,auto,home,rent));

            }
    }

    
    
}
