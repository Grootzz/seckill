package edu.uestc.service;


import edu.uestc.dao.UserDao;
import edu.uestc.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    UserDao userDao;


    public User getById(int id) {
        User user = userDao.getById(id);
        return user;
    }

    @Transactional
    public boolean tx() {
        User user = new User();
        user.setId(2);
        user.setName("cax");

        userDao.insert(user);

        User user1 = new User();
        user1.setId(1);
        user1.setName("fdfd");
        userDao.insert(user1);

        return true;
    }



}
