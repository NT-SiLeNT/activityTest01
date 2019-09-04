package com.activiti;


import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author wangxin
 * @Title
 * @Description
 * @date 2019-09-02 16:44
 */
public class Demo {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

    public static void main(String[] args) throws ParseException {
        LOGGER.info("启动程序");

        //1、创建流程引擎
        //1.1创建流程定义配置
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        //1.2创建流程引擎
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        LOGGER.info("流程引擎名称{}，版本{}",name,version);
        //2、部署流程文件
        //2.1获取service,对二进制文件操作，对流程配置库的操作
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //2.2获取Builder
        DeploymentBuilder deployment = repositoryService.createDeployment();
        deployment.addClasspathResource("vacation.bpmn20.xml");
        Deployment deploy = deployment.deploy();
        String id = deploy.getId();
        //更具id获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                                                    .deploymentId(id).singleResult();
        LOGGER.info("流程定义文件{}，流程ID{}",processDefinition.getName(),processDefinition.getId());
        //3、启动流程
        //3.1获取流程运行service
        RuntimeService runtimeService = processEngine.getRuntimeService();
        //3.2获取流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程{}",processInstance.getProcessDefinitionKey());
        //4、处理流程任务
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()){
            TaskService taskService = processEngine.getTaskService();
            //4.1获取处理的任务
            List<Task> list = taskService.createTaskQuery().list();
            LOGGER.info("待处理的任务数量【{}】",list.size());
            for (Task task : list){
                LOGGER.info("待处理任务【{}】",task.getName());
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                //获取表单属性
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                //准备流程参数
                Map<String,Object> variables = Maps.newHashMap();
                for (FormProperty formProperty : formProperties) {
                    //判断参数类型
                    String line = null;
                    if (StringFormType.class.isInstance(formProperty.getType())){
                        LOGGER.info("请输入{}？",formProperty.getName());
                        line = scanner.nextLine();
                        variables.put(formProperty.getId(),line);
                    }else if (DateFormType.class.isInstance(formProperty.getType())){
                        LOGGER.info("请输入{}？ 格式（yyyy-MM-dd）",formProperty.getName());
                        line = scanner.nextLine();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = dateFormat.parse(line);
                        variables.put(formProperty.getId(),date);
                    }else {
                        LOGGER.info("类型暂不支持{}",formProperty.getType());
                    }
                    LOGGER.info("您输入的内容是{}",line);
                }
                taskService.complete(task.getId(),variables);
                //更新流程实例状态
                processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }
        }
        LOGGER.info("结束程序");
    }
}
