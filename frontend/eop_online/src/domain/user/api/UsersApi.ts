import client from "../../../application/api/BaseClient"
import { Session } from "./../types/Session"
export default {
    fetchMeInfo(): Promise<Session> {
        return client.get<Session>("/users/me").then((resp) => resp.data)
    },
}
