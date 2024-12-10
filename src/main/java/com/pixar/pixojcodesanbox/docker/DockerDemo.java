package com.pixar.pixojcodesanbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;
// 注意这个代码在windows无法运行！！！
public class DockerDemo {

    public static void main(String[] args) throws InterruptedException {
        // 获取默认的Docker Clint
        DockerClient dockerClint = DockerClientBuilder.getInstance().build();
        //拉取镜像
        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClint.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
//            @Override
//            //使用异步时间非常长
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载状态：" + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd
//                .exec(pullImageResultCallback)
//                .awaitCompletion();//阻塞直到下载完成
//        System.out.println("下载完成");
        // 创建容器
        CreateContainerCmd containerCmd = dockerClint.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd
                .withCmd("echo","Hello Docker")
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        //查看容器状态
        ListContainersCmd listContainersCmd = dockerClint.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        for(Container container : containerList){
            System.out.println(container);
        }

        //启动容器
        dockerClint.startContainerCmd(containerId).exec();

        // Thread.sleep(10000L);

        //查看日志
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback(){
            @Override
            public void onNext(Frame item) {
                System.out.println("日志：" + new String(item.getPayload()));
                super.onNext(item);
            }
        };

        // 阻塞等待日志输出
        dockerClint.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();


    // 删除容器
    dockerClint.removeContainerCmd(containerId).withForce(true).exec();

    // 删除镜像
    dockerClint.removeImageCmd(image).exec();
    }

}
