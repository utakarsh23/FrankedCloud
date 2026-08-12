package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.Entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, ObjectId> {
    User findUserById(ObjectId id);
    User findUserByEmail(String email);
    User findUserByUsername(String username);
    User findUserByEmailOrUsername(String email, String username);
    User findUserByEmailAndPassword(String email, String password);
}
