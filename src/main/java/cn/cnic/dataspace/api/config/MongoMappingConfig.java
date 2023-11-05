package cn.cnic.dataspace.api.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * @ author jmal
 */
@Configuration
public class MongoMappingConfig {

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory, MongoMappingContext context, BeanFactory beanFactory) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        context.setAutoIndexCreation(false);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
        try {
            // Specify as MongoCustomimConversions. If the redis referenced by the project is: available: expected single matching bean but found 2: MongoCustomimConversions, redisCustomimConversions
            converter.setCustomConversions(beanFactory.getBean(MongoCustomConversions.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // don't save column _class to mongo collection
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
    // Mongo single node does not support transactions
    // @Bean
    // MongoTransactionManager transactionManager(MongoDbFactory factory){
    // return new MongoTransactionManager(factory);
    // }
}
