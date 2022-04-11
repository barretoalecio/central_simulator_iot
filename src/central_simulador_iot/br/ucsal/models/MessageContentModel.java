package central_simulador_iot.br.ucsal.models;

public class MessageContentModel {
	private String message;
	private Integer code;

	public MessageContentModel(String dataFromClient) {
		if (dataFromClient != null) {
			String[] splitedContent = dataFromClient.split(" ");
			this.message = splitedContent[0].toUpperCase();
			this.code = Integer.parseInt(splitedContent[1]);
		}
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return "MessageContentModel [message=" + message + ", code=" + code + ", getMessage()=" + getMessage()
				+ ", getCode()=" + getCode() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
				+ ", toString()=" + super.toString() + "]";
	}

}
