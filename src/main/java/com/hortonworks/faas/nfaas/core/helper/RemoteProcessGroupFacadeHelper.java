package com.hortonworks.faas.nfaas.core.helper;

import com.hortonworks.faas.nfaas.config.EntityState;
import com.hortonworks.faas.nfaas.core.ProcessGroup;
import com.hortonworks.faas.nfaas.core.ProcessGroupFlow;
import com.hortonworks.faas.nfaas.core.RemoteProcessGroup;
import org.apache.nifi.web.api.dto.PortDTO;
import org.apache.nifi.web.api.dto.RemoteProcessGroupDTO;
import org.apache.nifi.web.api.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RemoteProcessGroupFacadeHelper extends BaseFacadeHelper{

    private static final Logger logger = LoggerFactory.getLogger(RemoteProcessGroupFacadeHelper.class);


    /**
     * This is the method which is used to get all the remote process group from
     * the Pgfe
     *
     * @param pgfe
     * @param remoteProcessGroupsFromTemplate
     * @return
     */
    @SuppressWarnings("unused")
    private Set<RemoteProcessGroupEntity> getRemoteProcessGroupEntityForUndeploy(ProcessGroupFlowEntity pgfe,
                                                                                 Set<RemoteProcessGroupDTO> remoteProcessGroupsFromTemplate) {

        Set<RemoteProcessGroupEntity> resultRemotePG = new LinkedHashSet<>();
        Set<RemoteProcessGroupEntity> allRemoteProcessGroups = pgfe.getProcessGroupFlow().getFlow()
                .getRemoteProcessGroups();

        Set<String> remoteProcessorGroupNameFromTemplate = templateFacadeHelper.getAllRemoteProcessorGroupNameFromTemplate(
                remoteProcessGroupsFromTemplate);

        for (RemoteProcessGroupEntity rpge : allRemoteProcessGroups) {
            if (remoteProcessorGroupNameFromTemplate.contains(rpge.getComponent().getName())) {
                resultRemotePG.add(rpge);
            }

        }
        return resultRemotePG;
    }

    /**
     * This is the method to disable stop the process group.
     *
     * @param pgId
     * @param pgId
     */
    public void disableRemoteProcessGroup(String pgId) {
        logger.debug("disableRemoteProcessGroup Starts for --> " + pgId);

        ProcessGroupFlowEntity pgfe = processGroupFlow.getLatestProcessGroupFlowEntity(pgId);
        Set<ProcessGroupEntity> processGroups = pgfe.getProcessGroupFlow().getFlow().getProcessGroups();

        for (ProcessGroupEntity processGroupEntity : processGroups) {
            if (processGroupEntity.getActiveRemotePortCount() > 0) {
                disableRemoteProcessGroup(processGroupEntity.getId());
            }
        }

        ProcessGroupEntity pge =processGroup.getLatestProcessGroupEntity(pgId);
        RemoteProcessGroupsEntity remoteProcessGroupsEntity = remoteProcessGroup.getLatestRemoteProcessGroupsEntity(pgId);

        Set<RemoteProcessGroupEntity> remoteProcessGroups = remoteProcessGroupsEntity.getRemoteProcessGroups();

        if (remoteProcessGroups.isEmpty()) {
            logger.debug("No remote process group found for the PG " + pge.getComponent().getName());
            logger.debug("disableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());
            return;
        }

        for (RemoteProcessGroupEntity rpge : remoteProcessGroups) {
            logger.info("disableRemoteProcessGroup Starts for --> " + pge.getComponent().getName());
            this.disableRemoteProcessGroupComponents(rpge);
            logger.info("disableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());
        }
        pge = processGroup.getLatestProcessGroupEntity(pgId);
        logger.debug("disableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());

    }

    /**
     * This is the method which is used to disable the remote process group
     * componets
     *
     * @param remoteProcessGroupEntity
     * @return
     */
    private RemoteProcessGroupEntity disableRemoteProcessGroupComponents(
            RemoteProcessGroupEntity remoteProcessGroupEntity) {
        disableRemoteProcessGroupComponents(remoteProcessGroupEntity, EntityState.TRANSMIT_FALSE.getState());

        checkRemoteProcessGroupComponentsStatus(remoteProcessGroupEntity, EntityState.TRANSMIT_FALSE.getState());
        RemoteProcessGroupEntity rpge = remoteProcessGroup.getLatestRemoteProcessGroupEntity(remoteProcessGroupEntity.getId());
        return rpge;

    }


    /**
     * Check the remote Process Group Component Status
     *
     * @param remoteProcessGroupEntity
     * @param state
     */
    private RemoteProcessGroupEntity checkRemoteProcessGroupComponentsStatus(
            RemoteProcessGroupEntity remoteProcessGroupEntity, String state) {
        int count = 0;

        RemoteProcessGroupEntity rpge = null;

        while (true && count < WAIT_IN_SEC) {
            rpge = remoteProcessGroup.getLatestRemoteProcessGroupEntity(remoteProcessGroupEntity.getId());

            if (state.equalsIgnoreCase(String.valueOf(rpge.getComponent().isTransmitting())))
                break;

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {

            }

            count++;
        }

        return rpge;

    }

    /**
     * This is the method to disable stop the process group.
     *
     * @param pgId
     */
    private void enableRemoteProcessGroup(String pgId) {

        if (!remoteProcessGroup.isEnableRPG()) {
            logger.error("DEMO :: enable remote process group skipping ");
            return;
        }

        ProcessGroupFlowEntity pgfe = processGroupFlow.getLatestProcessGroupFlowEntity(pgId);
        Set<ProcessGroupEntity> processGroups = pgfe.getProcessGroupFlow().getFlow().getProcessGroups();

        for (ProcessGroupEntity processGroupEntity : processGroups) {
            if (processGroupEntity.getInactiveRemotePortCount() > 0) {
                enableRemoteProcessGroup(processGroupEntity.getId());
            }
        }

        ProcessGroupEntity pge = processGroup.getLatestProcessGroupEntity(pgId);
        RemoteProcessGroupsEntity remoteProcessGroupsEntity = remoteProcessGroup.getLatestRemoteProcessGroupsEntity(pgId);

        Set<RemoteProcessGroupEntity> remoteProcessGroups = remoteProcessGroupsEntity.getRemoteProcessGroups();

        if (remoteProcessGroups.isEmpty()) {
            logger.debug("No remote process group found for the PG " + pge.getComponent().getName());
            logger.debug("enableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());
            return;
        }

        for (RemoteProcessGroupEntity rpge : remoteProcessGroups) {
            logger.info("enableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());
            enableRemoteProcessGroupComponents(rpge);
            logger.info("enableRemoteProcessGroup Ends for --> " + pge.getComponent().getName());
        }
        pge = processGroup.getLatestProcessGroupEntity(pgId);


    }



    /**
     * This is the method which is used to delete all the remote process group
     * for the PG
     *
     * @param pgId
     */
    @SuppressWarnings("unused")
    private void deleteAllRemoteProcessGroup(String pgId) {
        logger.info("deleteAllRemoteProcessGroup Starts for --> " + pgId);
        ProcessGroupEntity pge = processGroup.getLatestProcessGroupEntity(pgId);
        RemoteProcessGroupsEntity remoteProcessGroupsEntity = remoteProcessGroup.getLatestRemoteProcessGroupsEntity(pgId);

        Set<RemoteProcessGroupEntity> remoteProcessGroups = remoteProcessGroupsEntity.getRemoteProcessGroups();

        if (remoteProcessGroups.isEmpty()) {
            logger.warn("No remote process group found for the PG " + pge.getComponent().getName());
            return;
        }

        for (RemoteProcessGroupEntity rpge : remoteProcessGroups) {
            remoteProcessGroup.deleteRemoteProcessGroupComponents(rpge);
        }
        pge = processGroup.getLatestProcessGroupEntity(pgId);
        logger.info("deleteAllRemoteProcessGroup Ends for --> " + pge.toString());

    }

    /**
     * Call the NIFI rest api to enable the process group
     *
     * @param remoteProcessGroupEntity
     *
     */
    private RemoteProcessGroupEntity enableRemoteProcessGroupComponents(
            RemoteProcessGroupEntity remoteProcessGroupEntity) {
        enableRemoteProcessGroupComponents(remoteProcessGroupEntity, EntityState.TRANSMIT_TRUE.getState());

        checkRemoteProcessGroupComponentsStatus(remoteProcessGroupEntity, EntityState.TRANSMIT_TRUE.getState());
        RemoteProcessGroupEntity rpge = remoteProcessGroup.getLatestRemoteProcessGroupEntity(remoteProcessGroupEntity.getId());
        return rpge;

    }
    /**
     * Call the NIFI rest api to disable the process group
     *
     * @param remoteProcessGroupEntity
     * @param state
     */
    private void disableRemoteProcessGroupComponents(RemoteProcessGroupEntity remoteProcessGroupEntity, String state) {
        remoteProcessGroup.enableOrDisableRemoteProcessGroupComponents(remoteProcessGroupEntity, state);
    }

    /**
     * Call the NIFI rest api to enable the process group
     *
     * @param remoteProcessGroupEntity
     * @param state
     */
    private void enableRemoteProcessGroupComponents(RemoteProcessGroupEntity remoteProcessGroupEntity, String state) {
        remoteProcessGroup.enableOrDisableRemoteProcessGroupComponents(remoteProcessGroupEntity, state);
    }

}