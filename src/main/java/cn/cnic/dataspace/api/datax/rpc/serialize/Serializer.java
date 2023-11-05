package cn.cnic.dataspace.api.datax.rpc.serialize;

/**
 * Serializer
 */
public abstract class Serializer {

    public abstract <T> byte[] serialize(T obj);

    public abstract <T> Object deserialize(byte[] bytes, Class<T> clazz);
    /*public enum SerializeEnum {
		HESSIAN(HessianSerializer.class),
		HESSIAN1(Hessian1Serializer.class);

		private Class<? extends Serializer> serializerClass;
		private SerializeEnum (Class<? extends Serializer> serializerClass) {
			this.serializerClass = serializerClass;
		}

		public Serializer getSerializer() {
			try {
				return serializerClass.newInstance();
			} catch (Exception e) {
				throw new XxlRpcException(e);
			}
		}

		public static SerializeEnum match(String name, SerializeEnum defaultSerializer){
			for (SerializeEnum item : SerializeEnum.values()) {
				if (item.name().equals(name)) {
					return item;
				}
			}
			return defaultSerializer;
		}
	}*/
}
