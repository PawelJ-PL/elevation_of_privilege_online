import { Box, Divider } from "@chakra-ui/layout"
import React, { useMemo } from "react"
import { KeyValues, Value } from "./ResponsiveTable"
import {
    COMPACT_TABLE_RECORD,
    COMPACT_TABLE_FIELD_HEADER,
    COMPACT_TABLE_FIELD_CONTENT,
    COMPACT_TABLE_FIELD_ENTRY,
} from "./testids"

type Props = { data: KeyValues[] }

const transformData = (data: KeyValues[]) => {
    const fieldNames = data.map((d) => d[0])
    const fieldValues = data.map((d) => d[1])
    const result: Array<Array<[string, Value]>> = []
    fieldValues.forEach((values, fieldIdx) => {
        values.forEach((value, valueIdx) => {
            const current = result[valueIdx] ?? []
            result[valueIdx] = [...current, [fieldNames[fieldIdx], value]]
        })
    })
    return result
}

const CompactTable: React.FC<Props> = ({ data }) => {
    const records = useMemo(() => transformData(data), [data])
    return (
        <Box>
            {records.map((r, idx) => (
                <Record key={idx} record={r} />
            ))}
        </Box>
    )
}

type RecordProps = { record: Array<[string, Value]> }

const Record: React.FC<RecordProps> = ({ record }) => (
    <Box borderWidth="1px" borderRadius="4px" marginBottom="0.5em" padding="0.5em" data-testid={COMPACT_TABLE_RECORD}>
        {record.map((r, idx) => (
            <Box key={idx} data-testid={COMPACT_TABLE_FIELD_ENTRY}>
                {idx > 0 && <Divider />}
                <Box fontWeight="bold" data-testid={COMPACT_TABLE_FIELD_HEADER}>
                    {r[0]}
                </Box>
                <Box data-testid={COMPACT_TABLE_FIELD_CONTENT}>{r[1]}</Box>
            </Box>
        ))}
    </Box>
)

export default CompactTable
