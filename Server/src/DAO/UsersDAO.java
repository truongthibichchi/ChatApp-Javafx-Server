package DAO;


import Util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import pojo.Users;

import java.util.List;

import static Util.HibernateUtil.getSession;

public class UsersDAO {
   public static Users getUser(String username) {
      Users user = null;
      Session session = getSession();
      try
      {
         user=session.get(Users.class,username);
      } catch (
              Exception ex)
      {
         System.err.println(ex);
      } finally
      {
         session.close();
      }
      return user;
   }

   public static List<Users> getUserList(){
      List<Users> userList = null;
      Session session = HibernateUtil.getSession();
      try{
         String hql="select user from Users user";
         Query query = session.createQuery(hql);
         userList=query.list();
      }catch (Exception ex){
         System.err.println(ex);
      }finally{
         session.close();
      }
      return userList;
   }
}
