import WebSocket from "ws"
import { Server as WsServer } from "ws"

const setupWebSocketServer = () => {
    const wss: WsServer = new WebSocket.Server({ port: 8181 })

    return { server: wss }
}

export const startServer = async () => {
    return setupWebSocketServer()
}

export const stopServer = (server: WsServer) => {
    return new Promise<void>((resolve, reject) =>
        server.close((err) => {
            if (err) {
                reject(err)
            } else {
                resolve()
            }
        })
    )
}
