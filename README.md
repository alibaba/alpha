![screenshot](/alpha_logo.png)

# Alpha启动框架
---

Alpha是一个基于PERT图构建的Android异步启动框架，它简单，高效，功能完善。  
在应用启动的时候，我们通常会有很多工作需要做，为了提高启动速度，我们会尽可能让这些工作并发进行。但这些工作之间可能存在前后依赖的关系，所以我们又需要想办法保证他们执行顺序的正确性。Alpha就是为此而设计的，使用者只需定义好自己的task，并描述它依赖的task，将它添加到Project中。框架会自动并发有序地执行这些task，并将执行的结果抛出来。  
由于Android应用支持多进程，所以Alpha支持为不同进程配置不同的启动模式。  


### 接入Alpha
使用gradle的方式:

```groovy
compile('com.alibaba.android:alpha:1.0.0@jar')
```

使用maven的方式:

```xml
<dependency>
    <groupId>com.alibaba.android</groupId>
    <artifactId>alpha</artifactId>
    <version>1.0.0</version>
    <type>jar</type>
</dependency>
```

### 使用指南
Alpha支持代码和配置文件的方式构建一个启动流程。
#### 使用Java代码构建

1.实现自己的Task类。继承Task类，在run()函数中实现该Task需要做的事情。

```java
	public class SampleTask extends Task{
        public SampleTask() {
            super("SampleTask");
        }

        @Override
        public void run() {
            //do something, print a msg for example.
            Log.d(TAG, "run SampleTask");
        }
    }
```
Task默认是在异步线程中执行的，如果这个Task需要在线程中执行，可以在构造函数中声明。  
2.将Task组合成一个完整的Project。  
可以用Task.ProjectBuilder依据各Task之间的依赖关系，将这些Task构建成一个完整的Project。

```java
		private Task createCommonTaskGroup() {
        Task a = new TaskA();
        Task b = new TaskB();
        Task c = new TaskC();
        Task d = new TaskD();
        Task e = new TaskE();

        Project.Builder builder = new Project.Builder();
        builder.add(a);
        builder.add(b).after(a);
        builder.add(c).after(a);
        builder.add(d).after(b, c);
        builder.add(e).after(a);
        Project group = builder.create();

        return group;
    }

```
ProjectBuilder生成的Project本身可以作为一个Task嵌入到另外一个Project中。
```java
    private Task createCommonTaskGroup() {
        Task a = new TaskA();
        Task b = new TaskB();
        Task c = new TaskC();
        Task d = new TaskD();
        Task e = new TaskE();

        Project.Builder builder = new Project.Builder();
        builder.add(a);
        builder.add(b).after(a);
        builder.add(c).after(a);
        builder.add(d).after(b, c);
        builder.add(e).after(a);
        Project group = builder.create();

        return group;
    }

    private void createProject() {
        Task group = createCommonTaskGroup();
        Task f = new TaskF();

        Project.Builder builder = new Project.Builder();
        builder.add(group);
        builder.add(f);

        Project project = builder.create();
    }
```
3.为构建完成的Project配置对应的进程。

```java
AlphaManager.getInstance(mContext).addProject(project);
```
4.执行启动流程
```java
AlphaManager.getInstance(mContext).start();
```

#### 使用XML配置文件构建
1.在代码中实现自己的Task，这个和上面的一样。
2.在XML文件中描述整个Project。

```xml
<projects>
    <project
            mode="mainProcess">
        <task
                name="TaskA"
                class="com.wireless.wireless.alpha.ConfigTest$TaskA"
                executePriority="8"
                mainThread="true"/>

        <task
                name="TaskB"
                class="com.wireless.wireless.alpha.ConfigTest$TaskB"
                predecessor="TaskA"/>

        <task
                name="TaskC"
                class="com.wireless.wireless.alpha.ConfigTest$TaskC"
                executePriority="4"
                predecessor="TaskA"/>

        <task
                name="TaskD"
                class="com.wireless.wireless.alpha.ConfigTest$TaskD"
                threadPriority="-5"
                predecessor="TaskB,TaskC"/>

        <task
                name="TaskE"
                class="com.wireless.wireless.alpha.ConfigTest$TaskE"/>

        <!--<task-->
                <!--name="TaskE"-->
                <!--class="com.wireless.koalainitializer.ConfigTest$TaskE"/>-->

        <task
                name="TaskF"
                class="com.wireless.wireless.alpha.ConfigTest$TaskF"
                mainThread="true"
                predecessor="TaskC"/>

        <task
                name="TaskG"
                class="com.wireless.wireless.alpha.ConfigTest$TaskG"
                threadPriority="-5"
                predecessor="TaskC"/>

    </project>



    <project
            mode="secondaryProcess">
        <task
                name="TaskA"
                class="com.wireless.wireless.alpha.ConfigTest$TaskA"
                mainThread="true"/>

        <task
                name="TaskB"
                class="com.wireless.wireless.alpha.ConfigTest$TaskB"
                predecessor="TaskA"/>

        <task
                name="TaskC"
                class="com.wireless.wireless.alpha.ConfigTest$TaskC"
                executePriority="8"
                predecessor="TaskA"/>

        <task
                name="TaskD"
                class="com.wireless.wireless.alpha.ConfigTest$TaskD"
                predecessor="TaskB,TaskC"/>


    </project>
</projects>
```
3.加载配置文件，这里我将该配置文件命名为tasklist.xml，并且放在asset中。

```java
		InputStream in = null;
        try {
        
            in = mContext.getAssets().open("tasklist.xml");
            AlphaManager.getInstance(mContext).addProjectsViaFile(in);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            AlphaUtils.closeSafely(in);
        }
```
4.执行启动流程

```java
AlphaManager.getInstance(mContext).start();
```
