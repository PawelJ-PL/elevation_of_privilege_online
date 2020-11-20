module.exports = {
    roots: ["../tests/"],
    globalSetup: "./setup.js",
    globalTeardown: "./teardown.js",
    testEnvironment: "./puppeteer_environment.js",
    transform: { "\\.ts$": ["ts-jest"] },
}
