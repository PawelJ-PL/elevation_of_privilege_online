import { useBreakpointValue } from "@chakra-ui/media-query"
import React from "react"
import CompactTable from "./CompactTable"
import StandardTable from "./StandardTable"

export type Value = string | JSX.Element | undefined | null

export type KeyValues = [string, Value[]]

type Breakpoint = "sm" | "md" | "lg" | "xl" | "2xl"

type Props = {
    data: KeyValues[]
    breakpointAt: Breakpoint
}

const ResponsiveTable: React.FC<Props> = ({ data, breakpointAt }) => {
    const tableType = useBreakpointValue({ base: "compact", [breakpointAt]: "standard" })
    if (!tableType) {
        return null
    } else if (tableType === "standard") {
        return <StandardTable data={data} />
    } else {
        return <CompactTable data={data} />
    }
}

export default ResponsiveTable
