import { useQueryClient } from '@tanstack/react-query'
import {
  getGetCapabilityContractQueryKey,
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
} from '@/api/endpoints/specs/specs'
import type { Capability } from '@/api/model'

// A facet toggle's after-mutation refresh, shared by AnswersFacet and PagingFacet: the
// contract, the spec detail, the spec list, and the jump-back-in pointer all re-fetch —
// the page re-renders from the invalidated projections, nothing echoed locally.
export function useContractInvalidation(
  specId: string,
  schemaName: string,
  capability: Capability,
) {
  const queryClient = useQueryClient()
  return function invalidate() {
    queryClient.invalidateQueries({
      queryKey: getGetCapabilityContractQueryKey(specId, schemaName, capability),
    })
    queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
    queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
    queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
  }
}
