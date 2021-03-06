package org.jlab.jaws;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.jlab.jaws.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

/**
 * Adds a Masked override to an alarm with an active parent alarm and removes the Masked override when the parent
 * alarm is no longer active.
 */
public class MaskRule extends AutoOverrideRule {

    private static final Logger log = LoggerFactory.getLogger(MaskRule.class);

    public static final String OUTPUT_TOPIC = "overridden-alarms";

    public static final String INPUT_TOPIC_REGISTERED = "registered-alarms";
    public static final String INPUT_TOPIC_ACTIVE = "active-alarms";

    public static final Serdes.StringSerde INPUT_KEY_REGISTERED_SERDE = new Serdes.StringSerde();
    public static final Serdes.StringSerde INPUT_KEY_ACTIVE_SERDE = new Serdes.StringSerde();

    public static final SpecificAvroSerde<RegisteredAlarm> INPUT_VALUE_REGISTERED_SERDE = new SpecificAvroSerde<>();
    public static final SpecificAvroSerde<ActiveAlarm> INPUT_VALUE_ACTIVE_SERDE = new SpecificAvroSerde<>();

    public static final SpecificAvroSerde<OverriddenAlarmKey> OUTPUT_KEY_SERDE = new SpecificAvroSerde<>();
    public static final SpecificAvroSerde<OverriddenAlarmValue> OUTPUT_VALUE_SERDE = new SpecificAvroSerde<>();

    public static final SpecificAvroSerde<MaskJoin> REGISTERED_ACTIVE_VALUE_SERDE = new SpecificAvroSerde<>();

    @Override
    public Properties constructProperties() {
        final Properties props = super.constructProperties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "jaws-auto-override-processor-mask");

        return props;
    }

    @Override
    public Topology constructTopology(Properties props) {
        final StreamsBuilder builder = new StreamsBuilder();

        // If you get an unhelpful NullPointerException in the depths of the AVRO deserializer it's likely because you didn't set registry config
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, props.getProperty(SCHEMA_REGISTRY_URL_CONFIG));

        INPUT_VALUE_REGISTERED_SERDE.configure(config, false);
        INPUT_VALUE_ACTIVE_SERDE.configure(config, false);

        OUTPUT_KEY_SERDE.configure(config, true);
        OUTPUT_VALUE_SERDE.configure(config, false);

        REGISTERED_ACTIVE_VALUE_SERDE.configure(config, false);

        final KTable<String, RegisteredAlarm> registeredTable = builder.table(INPUT_TOPIC_REGISTERED,
                Consumed.as("Registered-Table").with(INPUT_KEY_REGISTERED_SERDE, INPUT_VALUE_REGISTERED_SERDE));
        final KTable<String, ActiveAlarm> activeTable = builder.table(INPUT_TOPIC_ACTIVE,
                Consumed.as("Active-Table").with(INPUT_KEY_ACTIVE_SERDE, INPUT_VALUE_ACTIVE_SERDE));


        KTable<String, MaskJoin> maskJoined = registeredTable.join(activeTable, new MaskJoiner(), Materialized.with(Serdes.String(), REGISTERED_ACTIVE_VALUE_SERDE));

        // Only allow messages indicating an alarm is both active and has a maskedby to pass
        KStream<String, MaskJoin> filtered = maskJoined.toStream().filter(new Predicate<String, MaskJoin>() {
            @Override
            public boolean test(String key, MaskJoin value) {
                log.trace("filtering masked: {}={}", key, value);
                return value != null && value.getActive() && value.getMaskedby() != null;
            }
        });

        // Now map into overridden-alarms topic format
        final KStream<OverriddenAlarmKey, OverriddenAlarmValue> out = filtered.map(new KeyValueMapper<String, MaskJoin, KeyValue<? extends OverriddenAlarmKey, ? extends OverriddenAlarmValue>>() {
            @Override
            public KeyValue<? extends OverriddenAlarmKey, ? extends OverriddenAlarmValue> apply(String key, MaskJoin value) {
                return new KeyValue<>(new OverriddenAlarmKey(key, OverriddenAlarmType.Masked), new OverriddenAlarmValue(new MaskedAlarm()));
            }
        }, Named.as("Map-Mask"));

        final KStream<OverriddenAlarmKey, OverriddenAlarmValue> transformed = out
                .transform(new AddHeadersFactory());

        transformed.to(OUTPUT_TOPIC, Produced.as("Overridden-Alarms")
                .with(OUTPUT_KEY_SERDE, OUTPUT_VALUE_SERDE));

        return builder.build();
    }

    private final class MaskJoiner implements ValueJoiner<RegisteredAlarm, ActiveAlarm, MaskJoin> {

        public MaskJoin apply(RegisteredAlarm registered, ActiveAlarm active) {
            return MaskJoin.newBuilder()
                    .setActive(active != null)
                    .setMaskedby(registered.getMaskedby())
                    .build();
        }
    }
}
