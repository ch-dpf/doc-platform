package com.knowbase.autoconfigure;

import com.knowbase.web.controller.AclController;
import com.knowbase.web.controller.AgentVersionController;
import com.knowbase.web.controller.IngestionErrorController;
import com.knowbase.web.controller.IngestionRunController;
import com.knowbase.web.controller.KnowledgeAgentController;
import com.knowbase.web.controller.LibraryCatalogController;
import com.knowbase.web.controller.LibraryController;
import com.knowbase.web.controller.ObservabilityController;
import com.knowbase.web.controller.PresetController;
import com.knowbase.web.controller.QueryRunController;
import com.knowbase.web.controller.StorageController;
import com.knowbase.web.controller.TokenizerProfileController;
import com.knowbase.web.filter.KnowbaseRequestContextFilter;
import com.knowbase.web.support.KnowbaseExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;

@AutoConfiguration(after = KnowbaseAutoConfiguration.class)
@ConditionalOnClass({RestController.class, LibraryController.class})
@ConditionalOnProperty(prefix = "knowbase.web", name = "exposed", havingValue = "true")
@Import({
        LibraryController.class,
        LibraryCatalogController.class,
        IngestionRunController.class,
        IngestionErrorController.class,
        KnowledgeAgentController.class,
        AgentVersionController.class,
        PresetController.class,
        QueryRunController.class,
        TokenizerProfileController.class,
        AclController.class,
        StorageController.class,
        ObservabilityController.class,
        KnowbaseRequestContextFilter.class,
        KnowbaseExceptionHandler.class
})
public class KnowbaseWebAutoConfiguration {
}
