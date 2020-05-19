package io.quarkus.narayana.jta.deployment;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * 
 * Note: even though it would be much more appropriate to create
 * all the constants from statements containing <ClassName>.class.getName(),
 * the compiler does not allow us to pass in such values as default
 * parameters for {@link ConfigItem} annotated attributes.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public final class TransactionManagerBuildTimeConfiguration {

    private static final String XA_RESOURCE_ORPHAN_FILTERS = "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter"
            +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter" +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter" +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter";

    private static final String RECOVERY_MODULES = "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule" +
            ",com.arjuna.ats.internal.txoj.recovery.TORecoveryModule" +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule" +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule" +
            ",com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule";

    private static final String EXPIRY_SCANNERS = "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner";

    /**
     *
     */
    @ConfigItem(name = "JTAEnvironmentBean.xaResourceOrphanFilterClassNames", defaultValue = XA_RESOURCE_ORPHAN_FILTERS)
    public List<String> xaResourceOrphanFilterClassNames;

    /**
     * 
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryModuleClassNames", defaultValue = RECOVERY_MODULES)
    public List<String> recoveryModuleClassNames;

    /**
     * 
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.expiryScannerClassNames", defaultValue = EXPIRY_SCANNERS)
    public List<String> expiryScannerClassNames;

    /**
     * 
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryPort", defaultValue = "4712")
    public int recoveryPort;

    /**
     * 
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryListener", defaultValue = "true")
    public boolean recoveryListener;
}
