package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mapper.CourseMapper;
import com.example.mapper.EmpMapper;
import com.example.mapper.UserMapper;
import com.example.pojo.Course;
import com.example.pojo.Emp;
import com.example.pojo.User;
import com.example.util.OSSClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    OSSClientUtil ossClientUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private CourseMapper courseMapper;

    @PostMapping("/login")
    public ModelAndView login(ModelAndView mv,
                              @RequestParam(name = "userName") String userName,
                              @RequestParam(name = "password") String password, RedirectAttributes attr) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userName", userName);
        User user = userMapper.selectOne(userQueryWrapper);

        if (user != null && password.equals(user.getPassword())) {
            String image = user.getImage();
            mv.setViewName("userIndex");
            mv.addObject("user", user);
            mv.addObject("image", image);

            List<Course> courses = courseMapper.selectList(null);
            mv.addObject("courses", courses);

            return mv;
        } else {
            QueryWrapper<Emp> empQueryWrapper = new QueryWrapper<>();
            empQueryWrapper.eq("empName", userName);
            Emp emp = empMapper.selectOne(empQueryWrapper);
            if (emp != null && emp.getPassword().equals(password)) {
                mv.setViewName("empHomepage");
                mv.addObject("emp", emp);
                return mv;
            }
            mv.setViewName("redirect:/index");
            attr.addFlashAttribute("msg", "????????????,????????????????????????");
            return mv;
        }
    }

    /**
     * ????????????
     *
     * @param mv
     * @param userName
     * @param realName
     * @param password
     * @param sex
     * @param phone
     * @param attr
     * @return ModelAndView
     */
    @PostMapping("/registered")
    public ModelAndView registered(ModelAndView mv,
                                   @RequestParam(name = "userName") String userName,
                                   @RequestParam(name = "realName") String realName,
                                   @RequestParam(name = "passwordOne") String password,
                                   @RequestParam(name = "sex") String sex,
                                   @RequestParam(name = "phone") String phone,
                                   RedirectAttributes attr
    ) {
        User user = new User();
        user.setUserName(userName);
        user.setRealName(realName);
        user.setPassword(password);
        user.setSex(sex);
        user.setPhone(phone);
        user.setUserIdent("?????????");
        // ??????????????????
        user.setImage("https://health-club-management-system.oss-cn-beijing.aliyuncs.com/image/0000000000000.png");

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("userName")
                .eq("userName", userName);
        if (userMapper.selectCount(userQueryWrapper) >= 1) {
            mv.setViewName("redirect:/index");
            attr.addFlashAttribute("msg", "?????????????????????????????????");
            return mv;
        }
        userMapper.insert(user);
        mv.setViewName("redirect:/index");
        attr.addFlashAttribute("msg", "????????????????????????");
        return mv;
    }

    @RequestMapping("/info")
    public String userInfo(@RequestParam(name = "userName", required = true) String userName, ModelMap map) {

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userName", userName);
        List<User> users = userMapper.selectList(userQueryWrapper);
        for (User user : users) {
            map.addAttribute("user", user);
        }

        return "userCenter";
    }

    @PostMapping("/userUpdate")
    public String userUpdate(@RequestParam(value = "image") MultipartFile file,
                             @RequestParam(name = "userID", required = true) String userID,
                             @RequestParam(name = "userName", required = true) String userName,
                             @RequestParam(name = "password", required = true) String password,
                             @RequestParam(name = "sex", required = true) String sex,
                             @RequestParam(name = "phone", required = true) String phone,
                             ModelMap map
    ) {
        // ?????????????????????????????????user??????
        String msg = "";
        User user = userMapper.selectById(userID);
        user.setUserName(userName);
        user.setPassword(password);
        user.setSex(sex);
        user.setPhone(phone);
        if (file != null || file.getSize() > 0) {
            System.out.println("file's sizeof==???" + file.getSize());
            if (file.getSize() > 1024 * 1024 * 10) {
                msg += "??????????????????10MB!";
            } else {
                try {
                    user.setImage(ossClientUtil.updateHomeImage(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // ????????????????????????userName???????????????????????????
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userName", userName);
        List<User> users = userMapper.selectList(userQueryWrapper);

        // ???????????????userName??????????????????????????????
        if (users != null) {
            for (User entity : users) {
                // ????????????????????????ID??????????????????????????????ID??????????????????userName????????????????????????????????????
                if (entity.getUserID().toString().equals(userID)) {
                    System.out.println("id??????:" + entity.getUserID() + "==" + userID);
                    userMapper.updateById(user);
                } else {
                    System.out.println("id?????????:" + entity.getUserID() + "==" + userID);
                    // ????????????ID??????????????????????????????userName????????????????????????????????????????????????????????????
                    msg += " ????????????????????????????????????!";
                    map.addAttribute("msg", msg);
                    map.addAttribute("user", userMapper.selectById(userID));
                    return "userCenter";
                }
            }
        }
        // ???????????????userName??????????????????????????????????????????????????????userName????????????????????????????????????
        userMapper.updateById(user);
        map.addAttribute("msg", "????????????");
        map.addAttribute("user", user);
        return "userCenter";
    }

    // ??????????????????
    @RequestMapping("/{pageNum}")
    public ModelAndView userManager(@PathVariable("pageNum") Integer pageNum, ModelAndView modelAndView) {

        Page<User> page = new Page<>(pageNum, 10);
        userMapper.selectPage(page, null);
        if (page.getRecords().size() > 0) {
            modelAndView.setViewName("userManager");
            modelAndView.addObject("userIPage", page);
            modelAndView.addObject("pageCurrent", Math.toIntExact(page.getCurrent()));
            modelAndView.addObject("pages", Math.toIntExact(page.getPages()));
            modelAndView.addObject("Total", Math.toIntExact(page.getTotal()));
            // ????????????????????????flag
            modelAndView.addObject("flag", "normal");
            System.out.println("?????????" + page.getCurrent());
        } else {
            modelAndView.addObject("msg", "????????????");
        }
        return modelAndView;
    }

    // ????????????????????????
    @RequestMapping("/conditional")
    public ModelAndView workersManagerByConditional(ModelAndView modelAndView,
                                                    @RequestParam(name = "pageNum") Integer pageNum,
                                                    @RequestParam(name = "userID", defaultValue = "") String userID,
                                                    @RequestParam(name = "userName", defaultValue = "") String userName,
                                                    @RequestParam(name = "realName", defaultValue = "") String realName,
                                                    @RequestParam(name = "userIdent", defaultValue = "") String userIdent,
                                                    @RequestParam(name = "sex", defaultValue = "") String sex,
                                                    RedirectAttributes attr) {
        Page<User> page = new Page<>(pageNum, 10);
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (!userID.equals("")) {
            userQueryWrapper.eq("userID", userID);
        }
        if (!userName.equals("")) {
            userQueryWrapper.eq("userName", userName);
        }
        if (!realName.equals("")) {
            userQueryWrapper.eq("realName", realName);
        }
        if (!userIdent.equals("")) {
            userQueryWrapper.eq("userIdent", userIdent);
        }
        if (!sex.equals("")) {
            userQueryWrapper.eq("sex", sex);
        }

        userMapper.selectPage(page, userQueryWrapper);

        if (page.getRecords().size() > 0) {
            modelAndView.setViewName("userManager");
            modelAndView.addObject("userIPage", page);
            modelAndView.addObject("pageCurrent", Math.toIntExact(page.getCurrent()));
            modelAndView.addObject("pages", Math.toIntExact(page.getPages()));
            modelAndView.addObject("Total", Math.toIntExact(page.getTotal()));
            modelAndView.addObject("userID", userID);
            modelAndView.addObject("userName", userName);
            modelAndView.addObject("realName", realName);
            modelAndView.addObject("userIdent", userIdent);
            modelAndView.addObject("sex", sex);
            // ????????????????????????flag
            modelAndView.addObject("flag", "conditional");
            System.out.println("?????????" + page.getCurrent());
        } else {
            // ????????????????????? ??????????????????
            // ???????????????????????????
            System.out.println("??????????????????????????????");
            modelAndView.setViewName("redirect:/user/1");
            attr.addFlashAttribute("msg", "?????????????????????????????????????????????????????????");
        }
        return modelAndView;
    }

    @RequestMapping("/edit/{userID}")
    public ModelAndView userEdit(@PathVariable("userID") Long userID, ModelAndView modelAndView) {
        modelAndView.setViewName("userUpdate");
        User user = userMapper.selectById(userID);
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @RequestMapping("/update")
    public ModelAndView userUpdate(ModelAndView modelAndView,
                                   @RequestParam(name = "userID", required = true) Long userID,
                                   @RequestParam(name = "sex", required = true) String sex,
                                   @RequestParam(name = "userIdent", required = true) String userIdent,
                                   @RequestParam(name = "phone", required = true) String phone) {
        User user = new User();
        user.setUserID(userID);
        user.setSex(sex);
        user.setUserIdent(userIdent);
        user.setPhone(phone);
        int update = userMapper.updateById(user);
        if (update >= 1) {
            modelAndView.setViewName("redirect:/user/1");
            return modelAndView;
        }
        modelAndView.setViewName("userUpdate");
        modelAndView.addObject("user", userMapper.selectById(userID));
        modelAndView.addObject("msg", "???????????????");
        return modelAndView;
    }

    @RequestMapping("/delete/{userID}")
    public ModelAndView userDelete(ModelAndView modelAndView,
                                   @PathVariable("userID") Long userID,
                                   RedirectAttributes attr) {

        // ??????????????????????????????????????????????????????userID
        // ????????????????????????????????????????????????????????????
        try {
            userMapper.deleteById(userID);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            // ?????????????????????????????????????????????
            if (cause instanceof SQLIntegrityConstraintViolationException) {
                attr.addFlashAttribute("msg", "??????????????????????????????????????????????????????????????????????????????????????????");
                modelAndView.setViewName("redirect:/user/1");
                return modelAndView;
            }
        }
        modelAndView.setViewName("redirect:/user/1");
        return modelAndView;
    }
}
