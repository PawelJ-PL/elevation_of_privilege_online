/* eslint-disable no-undef */
/* eslint-disable @typescript-eslint/no-var-requires */

const { createProxyMiddleware } = require("http-proxy-middleware")

module.exports = function (app) {
    app.use(
        createProxyMiddleware("/api/v1/ws", {
            ws: true,
            target: "ws://localhost:8181",
        })
    )
    app.use(
        createProxyMiddleware("/api", {
            target: "http://localhost:8181",
        })
    )
}
