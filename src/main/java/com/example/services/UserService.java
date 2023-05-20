package com.example.services;

import com.example.annotations.RefreshCache;
import com.example.dto.UserDTO;
import com.example.entities.User;
import com.example.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.utils.AppConstants.*;

@Service
public class UserService implements IUserService{

    public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;


    @Value("${indicator}")
    private String indicator;

    public static String prop1;

    @Value("${property1}")
    public void setProp1(String p1){
        prop1 = p1;
    }

    public String getProp1(){
        return prop1;
    }

    public static final Function<Collection<User>, Map<Long, UserDTO>> CACHE_MAP_CONVERTER = (users) -> {
        Map<Long, UserDTO> userMap = users.stream().map(UserDTO::new).collect(Collectors.toConcurrentMap(UserDTO::getId, Function.identity(), (existing, newer) -> newer));
        return userMap;
    };

    @Override
    public Collection<UserDTO> findAll() {
        LOGGER.info("Indicator Value is : {} and prop1 is: {}", indicator, prop1);
        return userRepository.findAll().stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = USERS_BY_LOCATION, key="#location")
    public Map<Long, UserDTO> findByLocation(String location) {
        LOGGER.info("Calling findByLocation on UserRepository");
        Collection<User> users = userRepository.findByLocation(location);
        return CACHE_MAP_CONVERTER.apply(users);
    }

    @Override
    @Cacheable(value = USERS_BY_DEPARTMENT, key="#department")
    public Map<Long, UserDTO> findByDepartment(String department) {
        LOGGER.info("Calling findByLocation on UserRepository");
        Collection<User> users = userRepository.findByDepartment(department);
        return CACHE_MAP_CONVERTER.apply(users);
    }

    @Override
    @Cacheable(value = USERS_BY_LOCATION_AND_DEPARTMENT, key = "#location + '-' + #department")
    public Map<Long, UserDTO> findByLocationAndDepartment(String location, String department) {
        LOGGER.info("Calling findByLocationAndDepartment on UserRepository");
        Collection<User> users = userRepository.findByLocationAndDepartment(location, department);
        return CACHE_MAP_CONVERTER.apply(users);
    }

    @Override
    public UserDTO findById(Long id) {
        return userRepository.findById(id).map(UserDTO::new).orElseGet(() -> null);
    }

    @Override
    @RefreshCache(cacheNames = {USERS_BY_LOCATION, USERS_BY_DEPARTMENT, USERS_BY_LOCATION_AND_DEPARTMENT})
    public UserDTO save(User user) {
        UserDTO saved = new UserDTO(userRepository.save(user));
        return saved;
    }

    @Override
    @RefreshCache(cacheNames = {USERS_BY_LOCATION, USERS_BY_DEPARTMENT, USERS_BY_LOCATION_AND_DEPARTMENT})
    public UserDTO create(User user) {
        UserDTO saved = new UserDTO(userRepository.save(user));
        return saved;
    }

    @Override
    @RefreshCache(cacheNames = {USERS_BY_LOCATION, USERS_BY_DEPARTMENT, USERS_BY_LOCATION_AND_DEPARTMENT}, isDelete = "Y")
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
