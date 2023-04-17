package settings;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class SettingsModel {
    @JsonProperty("server_ports")
    public List<Integer> serverPorts;
    @JsonProperty("client_ports")
    public List<Integer> clientPorts;
    @JsonProperty("local_port")
    public Integer localPort;
}
