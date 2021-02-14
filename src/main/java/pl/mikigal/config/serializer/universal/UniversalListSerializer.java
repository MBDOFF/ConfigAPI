package pl.mikigal.config.serializer.universal;

import org.bukkit.configuration.ConfigurationSection;
import pl.mikigal.config.BukkitConfiguration;
import pl.mikigal.config.exception.InvalidConfigFileException;
import pl.mikigal.config.exception.MissingSerializerException;
import pl.mikigal.config.serializer.Serializer;
import pl.mikigal.config.serializer.Serializers;
import pl.mikigal.config.util.TypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper built-in serializer for processing List
 * @see List
 * @see Serializer
 * @since 1.0
 * @author Mikołaj Gałązka
 */
public class UniversalListSerializer extends Serializer<List> {

	@Override
	protected void saveObject(String path, List object, BukkitConfiguration configuration) {
		Class<?> generic = TypeUtils.getListGeneric(object);
		Serializer<?> serializer = Serializers.of(generic);
		if (serializer == null && !TypeUtils.isSimpleType(generic)) {
			throw new MissingSerializerException(generic);
		}

		configuration.set(path + ".type", generic.getName());
		for (int i = 0; i < object.size(); i++) {
			if (serializer == null) {
				configuration.set(path + "." + i, object.get(i));
				continue;
			}

			serializer.serialize(path + "." + i, object.get(i), configuration);
		}
	}

	@Override
	public List<?> deserialize(String path, BukkitConfiguration configuration) {
		ConfigurationSection section = configuration.getConfigurationSection(path);
		if (section == null || section.getKeys(false).size() == 0) {
			return new ArrayList<>();
		}

		String serializerClass = section.getString("type");
		if (serializerClass == null) {
			throw new InvalidConfigFileException("Serializer type is not defined for " + path);
		}

		Serializer<?> serializer = Serializers.of(serializerClass);

		try {
			if (serializer == null && !TypeUtils.isSimpleType(Class.forName(serializerClass))) {
				throw new MissingSerializerException(Class.forName(serializerClass));
			}
		} catch (ClassNotFoundException e) {
			throw new MissingSerializerException("Could not find class " + serializerClass);
		}

		List list = new ArrayList<>();
		for (String index : section.getKeys(false)) {
			if (index.equals("type")) {
				continue;
			}

			if (serializer == null) {
				list.add(configuration.get(path + "." + index));
				continue;
			}

			list.add(serializer.deserialize(path + "." + index, configuration));
		}

		return list;
	}
}
