import Axios from "axios"
import PackageJson from "../../../package.json"

const client = Axios.create()
client.defaults.withCredentials = true
client.defaults.baseURL = "/api/v1"
client.defaults.headers.common["Accept"] = "application/json"
client.defaults.headers.common["X-App-Version"] = "eop-web/" + PackageJson.version

export default client
