package io.quarkus.narayana.jta.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionScoped;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import com.arjuna.ats.internal.jta.resources.arjunacore.CommitMarkableResourceRecord;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ContextRegistrarBuildItem;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.narayana.jta.runtime.CDIDelegatingTransactionManager;
import io.quarkus.narayana.jta.runtime.NarayanaJtaProducers;
import io.quarkus.narayana.jta.runtime.NarayanaJtaRecorder;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import io.quarkus.narayana.jta.runtime.context.TransactionContext;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorMandatory;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNever;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequiresNew;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorSupports;

class NarayanaJtaProcessor {

    @BuildStep
    public NativeImageSystemPropertyBuildItem nativeImageSystemPropertyBuildItem() {
        return new NativeImageSystemPropertyBuildItem("CoordinatorEnvironmentBean.transactionStatusManagerEnable", "false");
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.TRANSACTIONS);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(NarayanaJtaRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<FeatureBuildItem> feature,
            TransactionManagerBuildTimeConfiguration transactionBuildTimeConfig,
            TransactionManagerConfiguration transactions) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_JTA));
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaJtaProducers.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(CDIDelegatingTransactionManager.class));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(CommitMarkableResourceRecord.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JTAEnvironmentBean.class.getName(),
                UserTransactionImple.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                TransactionManagerImple.class.getName(),
                TransactionSynchronizationRegistryImple.class.getName(),
                ObjectStoreEnvironmentBean.class.getName(),
                ShadowNoFileLockStore.class.getName()));

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(TransactionalInterceptorSupports.class);
        builder.addBeanClass(TransactionalInterceptorNever.class);
        builder.addBeanClass(TransactionalInterceptorRequired.class);
        builder.addBeanClass(TransactionalInterceptorRequiresNew.class);
        builder.addBeanClass(TransactionalInterceptorMandatory.class);
        builder.addBeanClass(TransactionalInterceptorNotSupported.class);
        additionalBeans.produce(builder.build());

        //we want to force Arjuna to init at static init time
        Properties defaultProperties = PropertiesFactory.getDefaultProperties();
        //we don't want to store the system properties here
        //we re-apply them at runtime
        //for (Object i : System.getProperties().keySet()) {
        //    defaultProperties.remove(i);
        //}
        recorder.setDefaultProperties(defaultProperties);
        // This must be done before setNodeName as the code in setNodeName will create a TSM based on the value of this property
        recorder.disableTransactionStatusManager();
        recorder.setNodeName(transactions);
        recorder.setDefaultTimeout(transactions);

        RecoveryEnvironmentBean recoveryEnvironmentBean = new RecoveryEnvironmentBean();
        JTAEnvironmentBean jtaEnvironmentBean = new JTAEnvironmentBean();
        CoreEnvironmentBean coreEnvironmentBean = new CoreEnvironmentBean();
        CoordinatorEnvironmentBean coordinatorEnvironmentBean = new CoordinatorEnvironmentBean();
        ObjectStoreEnvironmentBean objectStoreEnvironmentBean = new ObjectStoreEnvironmentBean();

        configJtaEnvironmentBean(jtaEnvironmentBean, transactionBuildTimeConfig);
        configRecoveryEnvironmentBean(recoveryEnvironmentBean, transactionBuildTimeConfig);

        Map<String, Object> configBeans = new HashMap<>();
        configBeans.put(CoordinatorEnvironmentBean.class.getName(), coordinatorEnvironmentBean);
        configBeans.put(RecoveryEnvironmentBean.class.getName(), recoveryEnvironmentBean);
        configBeans.put(CoreEnvironmentBean.class.getName(), coreEnvironmentBean);
        configBeans.put(JTAEnvironmentBean.class.getName(), jtaEnvironmentBean);
        configBeans.put(ObjectStoreEnvironmentBean.class.getName(), objectStoreEnvironmentBean);
        recorder.setConfigBeans(configBeans);
    }

    private void configJtaEnvironmentBean(JTAEnvironmentBean bean, TransactionManagerBuildTimeConfiguration config) {
        bean.setXaResourceOrphanFilterClassNames(config.xaResourceOrphanFilterClassNames);
    }

    private void configRecoveryEnvironmentBean(RecoveryEnvironmentBean bean, TransactionManagerBuildTimeConfiguration config) {
        bean.setRecoveryModuleClassNames(config.recoveryModuleClassNames);
        bean.setExpiryScannerClassNames(config.expiryScannerClassNames);
        bean.setRecoveryPort(config.recoveryPort);
        bean.setRecoveryListener(config.recoveryListener);
    }

    @BuildStep
    public void transactionContext(
            BuildProducer<ContextRegistrarBuildItem> contextRegistry) {

        contextRegistry.produce(new ContextRegistrarBuildItem(new ContextRegistrar() {
            @Override
            public void register(RegistrationContext registrationContext) {
                registrationContext.configure(TransactionScoped.class).normal().contextClass(TransactionContext.class).done();
            }
        }, TransactionScoped.class));
    }

}
