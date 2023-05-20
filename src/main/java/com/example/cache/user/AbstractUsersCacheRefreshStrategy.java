package com.example.cache.user;

import com.example.cache.CacheRefreshStrategy;
import com.example.dto.UserDTO;
import com.example.services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class AbstractUsersCacheRefreshStrategy extends CacheRefreshStrategy<String, UserDTO> {

    @Autowired
    IUserService userService;

    @Override
    public String cacheIdentifierField() {
        return "id";
    }

    @Override
    public Boolean isEvictionFromExistingCacheRequired(UserDTO existingObject) {
        return Objects.nonNull(existingObject);
    }

    @Override
    public UserDTO getExistingObjectByIdentifier(Object id) {
        return userService.findById((Long) id);
    }

}
