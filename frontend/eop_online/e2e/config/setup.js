const fs = require("fs")
const os = require("os")
const path = require("path")
const mkdirp = require("mkdirp")
const puppeteer = require("puppeteer")

const DIR = path.join(os.tmpdir(), "jest_puppeteer_global_setup")

module.exports = async function () {
    const browser = await puppeteer.launch({
        headless: process.env.WITH_BROWSER !== "true",
        args: ["--no-sandbox", "--disable-setuid-sandbox"],
    })
    // store the browser instance so we can teardown it later
    // this global is only available in the teardown but not in TestEnvironments
    global.__BROWSER__ = browser

    // use the file system to expose the wsEndpoint for TestEnvironments
    mkdirp.sync(DIR)
    fs.writeFileSync(path.join(DIR, "wsEndpoint"), browser.wsEndpoint())
}
