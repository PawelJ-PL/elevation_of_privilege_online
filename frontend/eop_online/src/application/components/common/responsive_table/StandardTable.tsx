import { Thead } from "@chakra-ui/react"
import { Table, Tbody, Td, Th, Tr } from "@chakra-ui/table"
import React from "react"
import { KeyValues, Value } from "./ResponsiveTable"
import unzip from "lodash/unzip"
import { STANDARD_TABLE_CELL, STANDARD_TABLE_ROW } from "./testids"

type Props = { data: KeyValues[] }

const StandardTable: React.FC<Props> = ({ data }) => {
    const rows = unzip(data.map((d) => d[1]))

    return (
        <Table variant="striped">
            <Thead>
                <Tr>
                    {data.map((field, idx) => (
                        <Th key={idx}>{field[0]}</Th>
                    ))}
                </Tr>
            </Thead>
            <Tbody>
                {rows.map((row, idx) => (
                    <Tr key={idx} data-testid={STANDARD_TABLE_ROW}>
                        <SingleRow values={row} />
                    </Tr>
                ))}
            </Tbody>
        </Table>
    )
}

type SingleRowProps = { values: Value[] }

const SingleRow: React.FC<SingleRowProps> = ({ values }) => (
    <>
        {values.map((cell, idx) => (
            <Td key={idx} data-testid={STANDARD_TABLE_CELL}>
                {cell}
            </Td>
        ))}
    </>
)

export default StandardTable
