import React from "react"
import ReactDOM from "react-dom"
import App from "./App"
import { ChakraProvider } from "@chakra-ui/react"
import DefaultTheme from "./themes/default/DefaultTheme"

ReactDOM.render(
    <React.StrictMode>
        <ChakraProvider resetCSS={true} theme={DefaultTheme}>
            <App />
        </ChakraProvider>
    </React.StrictMode>,
    document.getElementById("root")
)
