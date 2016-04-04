package config

import (
	"crypto/tls"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"time"

	"github.com/subutai-io/base/agent/log"

	"gopkg.in/gcfg.v1"
)

var client *http.Client

type agentConfig struct {
	Debug       bool
	GpgUser     string
	AppPrefix   string
	LxcPrefix   string
	DataPrefix  string
	GpgPassword string
}
type managementConfig struct {
	Cdn           string
	Host          string
	Port          string
	Login         string
	Secret        string
	Kurjun        string
	GpgUser       string
	Version       string
	Password      string
	RestToken     string
	RestVerify    string
	RestPublicKey string
}

type influxdbConfig struct {
	Server string
	Db     string
	User   string
	Pass   string
}
type templateConfig struct {
	Version string
	Arch    string
}
type configFile struct {
	Agent      agentConfig
	Management managementConfig
	Template   templateConfig
	Influxdb   influxdbConfig
}

const defaultConfig = `
	[agent]
	gpgUser =
	gpgPassword = 12345678
	debug = true
	appPrefix = /apps/subutai/current/
	dataPrefix = /var/lib/apps/subutai/current/
	lxcPrefix = /mnt/lib/lxc/

	[management]
	version = stable
	gpgUser =
	port = 8443
	host = 10.10.10.1
	login = internal
	password = secretSubutai
	secret = secret
	restToken = /rest/v1/identity/gettoken
	restPublicKey = /rest/v1/registration/public-key
	restVerify = /rest/v1/registration/verify/container-token
    cdn = cdn.subut.ai

	[influxdb]
	server = 10.10.10.1
	user = root
	pass = root
	db = metrics

	[template]
	version = 4.0.0
	arch = amd64
`

var (
	config configFile
	// Agent describes configuration options that used for configuring Subutai Agent
	Agent agentConfig
	// Management describes configuration options that used for accessing Subutai Management server
	Management managementConfig
	// Influxdb describes configuration options for InluxDB server
	Influxdb influxdbConfig
	// Template describes template configuration options
	Template templateConfig
)

func init() {
	log.Level(log.InfoLevel)

	err := gcfg.ReadStringInto(&config, defaultConfig)
	log.Check(log.InfoLevel, "Loading default config ", err)

	err = gcfg.ReadFileInto(&config, "/apps/subutai/current/etc/agent.gcfg")
	log.Check(log.WarnLevel, "Opening Agent config file /apps/subutai/current/etc/agent.gcfg", err)

	files, _ := ioutil.ReadDir("/apps/")
	for _, f := range files {
		if f.Name() == "subutai-mng" {
			config.Agent.AppPrefix = "/apps/subutai-mng/current/"
			config.Agent.DataPrefix = "/var/lib/" + config.Agent.AppPrefix
		}
	}

	name, _ := os.Hostname()
	config.Agent.GpgUser = name + "@subutai.io"

	Agent = config.Agent
	Influxdb = config.Influxdb
	Template = config.Template
	Management = config.Management
}

func InitAgentDebug() {
	if config.Agent.Debug {
		log.Level(log.DebugLevel)
	}
}

func CheckKurjun() (client *http.Client) {
	_, err := net.DialTimeout("tcp", Management.Host+":8338", time.Duration(3)*time.Second)
	if !log.Check(log.InfoLevel, "Trying local Kurjun", err) {
		Management.Kurjun = "https://" + Management.Host + ":8338/rest/kurjun"
		tr := &http.Transport{TLSClientConfig: &tls.Config{InsecureSkipVerify: true}}
		client = &http.Client{Transport: tr}
	} else {
		Management.Kurjun = "https://" + Management.Cdn + ":8338/kurjun/rest"
		client = &http.Client{}
	}
	return
}
