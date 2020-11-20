import { extendTheme } from "@chakra-ui/core"

const theme = {
    styles: {
        global: {
            body: {
                backgroundColor: "#dff0fe",
            },
        },
    },
    components: {
        Modal: {
            baseStyle: {
                overlay: {
                    backdropFilter: "blur(4px)",
                },
            },
        },
    },
}

export default extendTheme(theme)
