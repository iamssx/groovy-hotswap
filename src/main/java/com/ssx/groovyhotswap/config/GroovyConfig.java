package com.ssx.groovyhotswap.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;

import java.io.File;
import java.net.URL;
import java.util.Collection;

@Slf4j
@Configuration
@Import({ScriptFactoryPostProcessor.class})
public class GroovyConfig implements ApplicationContextAware {

    private static final String refreshCheckDelayAttr = "org.springframework.scripting.support.ScriptFactoryPostProcessor.refreshCheckDelay";
    private static final String language = "org.springframework.scripting.support.ScriptFactoryPostProcessor.language";
    private static final String beanClassName = "org.springframework.scripting.groovy.GroovyScriptFactory";

    /**
     * 脚本在CLASSPATH下的目录
     */
    @Value("${groovy.directory}")
    private String directory;
    /**
     * 检测脚本是否修改间隔
     */
    @Value("${groovy.refreshCheckDelay}")
    private int refreshCheckDelay;

    /**
     * 将GroovyScriptFactory相关的Bean真正实例化
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        //项目不能放在中文目录下，否则会找不到资源
        URL resource = Thread.currentThread().getContextClassLoader().getResource(this.directory);
        if (resource == null) {
            return;
        }
        String realDirectory = resource.getFile();
        File scriptDir = new File(realDirectory);
        if (StringUtils.isEmpty(realDirectory)) {
            return;
        }
        if (!scriptDir.exists()) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(scriptDir, new String[]{"java"}, true);

        files.forEach(file -> {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClassName(beanClassName);
            //刷新时间
            beanDefinition.setAttribute(refreshCheckDelayAttr, refreshCheckDelay);
            //语言脚本
            beanDefinition.setAttribute(language, "groovy");
            //文件目录
            String filePath = file.getPath();
            String scriptLocator = filePath.substring(filePath.indexOf("\\" + directory + "\\") + 1);
            log.info("Register Groovy Script: {}", scriptLocator);
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, "classpath:" + scriptLocator);
            //注册到spring容器
            beanFactory.registerBeanDefinition(file.getName().replace(".java", ""), beanDefinition);
        });
    }

}
