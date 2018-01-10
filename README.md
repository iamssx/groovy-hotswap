# groovy-hotswap
this is a project using spring feature of groovy to refresh service class

通过利用spring支持的动态语言特性，来实现动态刷新spring中的bean达到热更的目的

### 依赖导入
因为要用到groovy，所以当然要将groovy的依赖导入
本程序用的是maven
```
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>2.4.12</version>
</dependency>
```

### Groovy配置类
```
//GroovyConfig.java
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
            String beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, file.getName().replace(".java", ""));
            beanFactory.registerBeanDefinition(beanName, beanDefinition);
        });
    }

}

//application.properties
groovy.directory=groovy
groovy.refreshCheckDelay=10000
#logging.level.org.springframework = debug

```
ScriptFactoryPostProcessor类是处理脚本的bean，Spring 通过一个与 ScriptFactoryPostProcessor bean 结合的 ScriptFactory 实现（在这里是一个 Groovy 工厂）创建脚本对象。ScriptFactoryPostProcessor bean 负责用由工厂创建的实际对象替换工厂 bean。
    当 Spring 装载应用程序上下文时，它首先创建工厂 bean（例如 GroovyScriptFactory bean）。然后，执行 ScriptFactoryPostProcessor bean，用实际的脚本对象替换所有的工厂 bean。在这里可以看到加载的bean的setBeanClassName都是org.springframework.scripting.groovy.GroovyScriptFactory，然后ScriptFactoryPostProcessor再将GroovyScriptFactory替换成脚本对象
    
### 编写业务类
首先建立一个GroovyService接口
然后，**在main目录下，建立groovy/com/ssx/groovyhotswap/service/impl/GroovyServiceImpl类，继承GroovyService，且此目录Unmark Sources Root**,
因为脚本类不能编译成class文件到执行目录classes下，只能是java源文件。
GroovyServiceImpl类有没有@Service注解都是无所谓的

### 编写pom文件
配置maven插件来实现清除执行目录下的脚本和将groovy目录下的脚本复制到执行目录下
```
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-clean-plugin</artifactId>
  <executions>
    <!--==========清空groovy目录下的类文件=============-->
    <execution>
      <goals>
      <goal>clean</goal>
      </goals>
      <configuration>
        <filesets>
          <fileset>
            <directory>target/classes/groovy</directory>
          </fileset>
        </filesets>
      </configuration>
    </execution>
  </executions>
</plugin>

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-resources-plugin</artifactId>
  <version>2.6</version>
  <executions>
    <!--==============将groovy目录下的文件复制到运行目录下===============-->
    <execution>
      <id>copy-resource</id>
      <phase>compile</phase>
      <goals>
        <goal>copy-resources</goal>
      </goals>
      <configuration>
        <encoding>UTF-8</encoding>
        <outputDirectory>target/classes/groovy</outputDirectory>
        <overwrite>true</overwrite>
        <resources>
          <resource>
            <directory>src/main/groovy</directory>
            <filtering>true</filtering>
          </resource>
          </resources>
      </configuration>
    </execution>
  </executions>
</plugin>
```
### 执行

- 先执行maven的clean命令，清空上次编译的编码
- 再执行maven的compile命令，编译代码，并把groovy目录下的groovy脚本代码放到执行目录classpath下
- 运行springboot的启动类
- 打开localhost:8080，会看到hotswap-version-1.0
- 打开执行目录classes下的groovy/com/ssx/groovyhotswap/service/impl/GroovyServiceImpl.java，将hotswap-version-1.0改成hotswap-version-2.0
- 再打开localhost:8080，发现变成hotswap-version-2.0啦！
