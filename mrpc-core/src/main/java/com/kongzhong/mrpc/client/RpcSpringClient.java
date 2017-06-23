package com.kongzhong.mrpc.client;

import com.kongzhong.mrpc.Const;
import com.kongzhong.mrpc.enums.RegistryEnum;
import com.kongzhong.mrpc.exception.SystemException;
import com.kongzhong.mrpc.interceptor.RpcClientInteceptor;
import com.kongzhong.mrpc.model.ClientBean;
import com.kongzhong.mrpc.model.RegistryBean;
import com.kongzhong.mrpc.registry.DefaultDiscovery;
import com.kongzhong.mrpc.registry.ServiceDiscovery;
import com.kongzhong.mrpc.utils.CollectionUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * RPC客户端
 *
 * @author biezhi
 *         2017/4/25
 */
@NoArgsConstructor
@Slf4j
public class RpcSpringClient extends SimpleRpcClient implements ApplicationContextAware, InitializingBean {

    private ApplicationContext ctx;

    @Override
    public void afterPropertiesSet() throws Exception {

        // 注册中心
        Map<String, RegistryBean> registryBeanMap = ctx.getBeansOfType(RegistryBean.class);
        if (CollectionUtils.isNotEmpty(registryBeanMap)) {
            registryBeanMap.values().forEach(registryBean -> serviceDiscoveryMap.put(registryBean.getName(), this.parseRegistry(registryBean)));
            this.usedRegistry = true;
        }

        // 客户端拦截器
        Map<String, RpcClientInteceptor> inteceptorMap = ctx.getBeansOfType(RpcClientInteceptor.class);
        if (CollectionUtils.isNotEmpty(inteceptorMap)) {
            inteceptorMap.values().forEach(super::addInterceptor);
        }

        // 客户端引用
        Map<String, ClientBean> clientBeanMap = ctx.getBeansOfType(ClientBean.class);

        ConfigurableApplicationContext context = (ConfigurableApplicationContext) ctx;
        DefaultListableBeanFactory dbf = (DefaultListableBeanFactory) context.getBeanFactory();

        super.init();

        if (CollectionUtils.isNotEmpty(clientBeanMap)) {
            clientBeanMap.values().forEach(clientBean -> super.initReferer(clientBean, dbf));
        }

        // 初始化引用
        referers.forEach(referer -> super.initReferer(referer, dbf));

        super.directConnect();

        log.info("Bind services finished, mrpc version [{}]", Const.VERSION);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

    protected ServiceDiscovery parseRegistry(RegistryBean registryBean) {
        String type = registryBean.getType();
        if (RegistryEnum.DEFAULT.getName().equals(type)) {
            return new DefaultDiscovery();
        }
        try {
            if (RegistryEnum.ZOOKEEPER.getName().equals(type)) {
                String zkAddr = registryBean.getAddress();
                Object zookeeperDiscovery = Class.forName("com.kongzhong.mrpc.discover.ZookeeperServiceDiscovery").getConstructor(String.class).newInstance(zkAddr);
                ServiceDiscovery serviceDiscovery = (ServiceDiscovery) zookeeperDiscovery;
                return serviceDiscovery;
            }
        } catch (Exception e) {
            throw new SystemException("Parse ServiceDiscovery error", e);
        }
        return null;
    }

}