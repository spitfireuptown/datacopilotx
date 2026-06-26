function formatEnum(ele: any) {
  return {
    value: ele,
    label: `permission.filter_${ele.replace(' ', '_')}`,
  }
}

function toLine(name: any) {
  return name.replace(/([A-Z])/g, '_$1').toLowerCase()
}

const textEnum = [
  'eq',
  'not_eq',
  'in',
  'not in',
  'like',
  'not like',
  'null',
  'not_null',
  'empty',
  'not_empty',
]
const textOptions = textEnum.map(formatEnum)

const dateEnum = ['eq', 'not_eq', 'lt', 'gt', 'le', 'ge']
const dateOptions = dateEnum.map(formatEnum)
const allEnum = [...new Set(textEnum.concat(dateEnum))]
const allOptions = allEnum.map(formatEnum)

const valueEnum = [...dateEnum]
const valueOptions = valueEnum.map(formatEnum)

const sysParams = ['eq', 'not_eq', 'like', 'not like', 'in', 'not in']
const textOptionsForSysParams = sysParams.map(formatEnum)

const sysParamsEnum = ['userId', 'userName', 'userEmail']

const sysParamsIlns = sysParamsEnum.map((_) => {
  return { value: `\${sysParams.${_}}`, label: `auth.sysParams_type.${toLine(_)}`, type: 'text' }
})

const fieldEnums = ['text', 'time', 'value', 'value', 'value', 'location']

export {
  textOptions,
  dateOptions,
  valueOptions,
  textOptionsForSysParams,
  sysParamsIlns,
  fieldEnums,
  allOptions,
}